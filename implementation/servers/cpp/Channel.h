#pragma once

class Channel;
#include "User.h"

#include <string>
#include <unordered_map>

#include <boost/shared_ptr.hpp>

class Channel
{
    std::unordered_map<std::string, boost::shared_ptr<User>> users;
    std::string name;

    void send_all(const std::string &source, const std::string &type, const std::string &message);

public:
    Channel(const std::string &name);
    const std::string& get_name() const;
    const std::unordered_map<std::string, boost::shared_ptr<User>> get_users() const;

    void handle_join(boost::shared_ptr<User> user);
    void handle_part(boost::shared_ptr<User> user, const std::string& message);
    void handle_nick(boost::shared_ptr<User> user, const std::string& new_nick);
    void handle_quit(boost::shared_ptr<User> user, const std::string& message);

    void send_message(const std::string &source, const std::string &message);
};
