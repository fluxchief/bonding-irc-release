#include "Channel.h"
#include <boost/algorithm/string.hpp>

Channel::Channel(const std::string &name): name{name} {

}

const std::string&Channel::get_name() const {
    return name;
}

const std::unordered_map<std::string, boost::shared_ptr<User>> Channel::get_users() const {
    return users;
}

void Channel::handle_join(boost::shared_ptr<User> user) {
    // Add the client to the internal user list
    users.insert({user -> get_name(), user});

    // Write all that the user joined
    send_all(user -> get_ident(), "JOIN", "");

    // Build a userlist
    std::string userlist;
    for(auto& u: users) {
        userlist += u.second -> get_name() + " ";
    }

    std::string m = user -> get_name() + " @ " + name;
    std::string n = user -> get_name() + " " + name;

    user -> send_message(353, m, userlist);
    user -> send_message(366, n, std::string("End of /NAMES list."));
}

void Channel::handle_part(boost::shared_ptr<User> user, const std::string& message) {
    send_all(user -> get_ident(), "PART", message);
    users.erase(user -> get_name());
}

void Channel::send_all(const std::string &source, const std::string &type, const std::string &message) {
    for (auto &it : users) {
        it.second -> send_message(source, type, name, message);
    }
}

void Channel::send_message(const std::string &source, const std::string &message) {
    // Send message to all but self
    for (auto &it : users) {
        auto &user = it.second;

        if(it.second -> get_ident() != source) {
            user -> send_message(source, "PRIVMSG", name, message);
        }
    }
}

void Channel::handle_nick(boost::shared_ptr<User> user, const std::string& new_nick) {
    for(auto &it: users) {
        it.second -> send_message(user -> get_ident(), "NICK", new_nick, "");
    }
}

void Channel::handle_quit(boost::shared_ptr<User> user, const std::string& message) {
    for(auto &it: users) {
        it.second -> send_message(user -> get_ident(), "QUIT", "", message);
    }

    users.erase(user -> get_name());
}
