#pragma once
#include <string>
#include <unordered_map>
#include <deque>
#include <vector>
#include <functional>

#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/enable_shared_from_this.hpp>

class User;
#include "Channel.h"
#include "Server.h"

namespace State{
    enum type_t {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    };
};

class User: public boost::enable_shared_from_this<User>
{
    boost::asio::streambuf response;
    boost::asio::ip::tcp::socket socket;
    boost::asio::ip::tcp::resolver resolver;

    std::string nickname;
    std::string hostname;

    std::unordered_map<std::string, boost::shared_ptr<Channel>> channels;
    Server* server;

    using irc_message_handler = std::function<int(const std::vector<std::string> &)>;
    std::unordered_map<std::string, irc_message_handler> irc_message_handlers;

    // Write message queue
    std::deque<std::string> out_message_queue;

    // Parse incoming message
    std::pair<std::string, std::vector<std::string>> parse_line(std::string line);

    void start_reading();
    void write_raw(const std::string &message);
    void write_welcome_message();
    State::type_t state;

public:
    User(boost::asio::io_service &io_service, Server *server);
    const std::string& get_name() const {
        return nickname;
    }

    const std::string get_ident() const {
        return nickname + std::string{"!x@"} + hostname;
    }

    const std::string& get_hostname() const;
    void join(boost::shared_ptr<Channel> channel);

    boost::asio::ip::tcp::socket& get_socket() {
        return socket;
    }

    // User has been started from the server
    void start();

    void send_message(const std::string &source, const std::string &type, const std::string &param, const std::string &message);
    void send_message(int code, const std::string &param, const std::string &message);
    void send_private_message(const std::string &source, const std::string &message);

    // Boost method handler
    void handle_message(const boost::system::error_code &error);
    void handle_write(const boost::system::error_code &error);

    // IRC message handlers
    int irc_handle_join(const std::vector<std::string> &parameters);
    int irc_handle_nick(const std::vector<std::string> &parameters);
    int irc_handle_part(const std::vector<std::string> &parameters);
    int irc_handle_ping(const std::vector<std::string> &parameters);
    int irc_handle_privmsg(const std::vector<std::string> &parameters);
    int irc_handle_quit(const std::vector<std::string> &parameters);
    int irc_handle_user(const std::vector<std::string> &parameters);
};
