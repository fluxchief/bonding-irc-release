#include "User.h"

#include <boost/algorithm/string.hpp>
#include <boost/make_shared.hpp>

#include <algorithm>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <sstream>
#include <vector>

User::User(boost::asio::io_service &io_service, Server *server):
    socket{io_service}, server{server}, nickname{}, hostname{}, resolver{io_service} {
    irc_message_handlers =
    {
        { "PING",       std::bind(&User::irc_handle_ping,      this, std::placeholders::_1) },
        { "NICK",       std::bind(&User::irc_handle_nick,      this, std::placeholders::_1) },
        { "PART",       std::bind(&User::irc_handle_part,      this, std::placeholders::_1) },
        { "JOIN",       std::bind(&User::irc_handle_join,      this, std::placeholders::_1) },
        { "PRIVMSG",    std::bind(&User::irc_handle_privmsg,   this, std::placeholders::_1) },
        { "QUIT",       std::bind(&User::irc_handle_quit,      this, std::placeholders::_1) },
        { "USER",       std::bind(&User::irc_handle_user,      this, std::placeholders::_1) }
    };
}

void User::send_message(int code, const std::string &param, const std::string &message) {
    std::ostringstream type;
    type << std::setw(3) << std::setfill('0') << code;

    send_message(server -> get_servername(), type.str(), param, message);
}

void User::send_message(const std::string &source, const std::string &type, const std::string &param, const std::string &message) {
    std::ostringstream buf;
    buf << ":" << source << " " << type << " " << param;

    if(!message.empty())
        buf <<  " :" << message;

    buf << std::endl;

    write_raw(buf.str());
}

void User::send_private_message(const std::string &source, const std::string &message) {
    send_message(source, "PRIVMSG", nickname, message);
}

void User::join(boost::shared_ptr<Channel> channel) {
    if(channels.find(channel -> get_name()) != channels.end()) {
        // Join the channel
        channel -> handle_join(shared_from_this());
        channels.insert({channel -> get_name(), channel});
    }
}

void User::write_raw(const std::string &message) {
    bool messages_pending = !out_message_queue.empty();
    out_message_queue.push_back(message);

    if(!messages_pending) {
        auto out_buffer = boost::asio::buffer(out_message_queue.front().data(), out_message_queue.front().length());
        boost::asio::async_write(
                socket,
                out_buffer,
                boost::bind(&User::handle_write, shared_from_this(), boost::asio::placeholders::error)
        );
    }
}

void User::start_reading() {
    boost::asio::async_read_until(
            socket,
            response,
            "\n",
            boost::bind( // same as std::bind?
                    &User::handle_message,
                    shared_from_this(),
                    boost::asio::placeholders::error));
}

void User::start() {
    boost::asio::ip::tcp::resolver::iterator destination = resolver.resolve(socket.remote_endpoint());

    hostname = destination -> host_name();
    start_reading();
}


std::pair<std::string, std::vector<std::string>> User::parse_line(std::string line) {
    // Parse message
    std::string command;
    std::string t_parameter;
    std::vector<std::string> parameter;

    // Trim line
    boost::algorithm::trim(line);

    auto pos = line.find(" ");
    if (pos != std::string::npos) {
        command = line.substr(0, pos);
        t_parameter = line.substr(pos + 1, line.length() - (pos + 1));
    } else {
        command = line;
    }

    // Convert parameters to a vector of parameter
    if(!t_parameter.empty()) {
        // do while instead?
        pos = t_parameter.find(" ");

        while (pos != std::string::npos && t_parameter[0] != ':') {
            std::string param{ t_parameter.substr(0, pos) };
            boost::algorithm::trim(param);

            parameter.push_back(std::move(param));

            t_parameter = t_parameter.substr(pos + 1);
            pos = t_parameter.find(" ");
        }

        if(!t_parameter.empty() && t_parameter[0] == ':')
            t_parameter = t_parameter.substr(1);

        parameter.push_back(t_parameter);
    }

    // Convert command to uppercase
    std::transform(command.begin(), command.end(), command.begin(), ::toupper);

    return std::make_pair(command, parameter);
}

void User::handle_message(const boost::system::error_code &error) {
    int rc;
    if(!error) {
        // Convert buffer to string
        std::string input(buffers_begin(response.data()), buffers_end(response.data()));
        response.consume(response.size());

        // Convert to a stream
        std::stringstream temp(input);
        std::string s_response;
        // Read line by line
        while(std::getline(temp, s_response)) {
            std::string command;
            std::vector<std::string> parameter;

            std::tie(command, parameter) = parse_line(s_response);

            std::cout << "Received command " << command << std::endl;

            // Handle commands
            auto handler = irc_message_handlers.find(command);
            if (handler != irc_message_handlers.end()) {
                auto &function = handler -> second;
                rc = function(parameter);

                if(rc < 0)
                    return;
            } else {
                std::cerr << "Command " << command << " not found" << std::endl;
                send_message(421, command, "Unknown command");
            }

        }
        start_reading();
    }
}

void User::handle_write(const boost::system::error_code &error) {
    if(!error) {
        // Remove sent message
        out_message_queue.pop_front();

        if(!out_message_queue.empty())  {
            // Send next message
            auto out_buffer = boost::asio::buffer(out_message_queue.front().data(), out_message_queue.front().length());
            boost::asio::async_write(
                    socket,
                    out_buffer,
                    boost::bind(&User::handle_write, shared_from_this(), boost::asio::placeholders::error)
            );
        }
    }
}

int User::irc_handle_nick(const std::vector<std::string> &parameters) {
    const char invalid_characters[] = " @!";
    if (parameters.empty()) {
        send_message(432, "Unset", "Invalid parameters size");
        std::cout << "bad param size " << parameters.size() << std::endl;
        return 0;
    }

    std::string nick{parameters[0]};

    if(nick == nickname){
        // We're already named like this - ignore
        return 0;
    }

    bool valid_nick = std::all_of(nick.cbegin(), nick.cend(), [&invalid_characters](char c) -> bool {
            for (int i = 0; i < strlen(invalid_characters); ++i)
            {
                if (c == invalid_characters[i])
                    return false;
            }

            return true;
        }
    );

    if(nick[0] == '#')
        valid_nick = false;

    if(!valid_nick) {
        std::cerr << "nick " << nick << " errorneus" << std::endl;
        send_message(432, nick, "Erroneus nickname");
        return 0;
    }

    if(server -> get_user(nick)) {
        std::cerr << "Nick in use" << std::endl;
        send_message(433, nick, "Nickname is already in use");
        return 0;
    }

    if(!nickname.empty()) {
        // Inform the client that the nick has been changed
        send_message(get_ident(), "NICK", nick, "");

        // Also inform all channels
        for(auto& c: channels)
            c.second -> handle_nick(shared_from_this(), nick);
    }

    nickname = nick;

    // Make sure to write the welcome message if the nick was changed
    write_welcome_message();
    return 0;
}

int User::irc_handle_ping(const std::vector<std::string> &parameters) {
    if(state != State::CONNECTED)
        return 0;

    send_message(server -> get_servername(), "PONG", parameters[0], "");
    return 0;
}

int User::irc_handle_part(const std::vector<std::string> &parameters) {
    if(state != State::CONNECTED)
        return 0;

    auto channel = channels.find(parameters[0]);
    if(channel != channels.end()) {
        // Our part message should not contain the channel name
        std::vector<std::string> quit_message = parameters;
        quit_message.erase(quit_message.begin());
        std::string message = boost::algorithm::join(quit_message, " ");

        // Part with the specified message
        channel -> second -> handle_part(shared_from_this(), message);

        // We're no longer in this channel
        channels.erase(parameters[0]);
    }

    return 0;
}

int User::irc_handle_join(const std::vector<std::string> &parameters) {
    if(state != State::CONNECTED)
        return 0;

    std::string channel_name = parameters[0];

    if(channel_name.empty() || channel_name[0] != '#' || channel_name.find(' ') != std::string::npos) {
        send_message(479, channel_name, "Illegal channel name");
        return 0;
    }

    if (channels.find(channel_name) != channels.end()) {
        return 0;
    }

    // Join channel
    auto channel = server -> get_channel(channel_name);
    if(!channel) {
        // Create new channel
        channel = boost::make_shared<Channel>(channel_name);
        server -> add_channel(channel);
    }

    channel -> handle_join(shared_from_this());
    channels.insert({channel_name, channel});

    return 0;
}

int User::irc_handle_privmsg(const std::vector<std::string> &parameters) {
    if(state != State::CONNECTED)
        return 0;

    std::string target = parameters[0];

    auto user = server -> get_user(target);
    auto channel = server -> get_channel(target);

    if(user) {
        user -> send_private_message(get_ident(), parameters[1]);
    } else if(channel) {
        channel -> send_message(get_ident(), parameters[1]);
    }

    return 0;
}

int User::irc_handle_quit(const std::vector<std::string> &parameters) {
    for(auto& c: channels) {
        c.second -> handle_quit(
            shared_from_this(), boost::algorithm::join(parameters, " ")
        );
    }

    channels.clear();
    server -> remove_user(shared_from_this());
    state = State::DISCONNECTED;
    return -1;
}

int User::irc_handle_user(const std::vector<std::string> &parameters) {
    return 0;
}

void User::write_welcome_message() {
    // Some irc clients depend on those messages (especially 001 and 376)
    if(state == State::CONNECTING) {
        std::ostringstream welcome_msg, hostname_msg;
        welcome_msg << "Welcome to " << server -> get_servername();
        send_message(1, nickname, welcome_msg.str());

        hostname_msg << "We found your hostname: " << hostname;
        send_message(2, nickname, hostname_msg.str());
        send_message(375, nickname, "Start MOTD");
        send_message(376, nickname, "End of MOTD");
        state = State::CONNECTED;
    }
}
