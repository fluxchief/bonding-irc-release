#pragma once

class Server;
#include "Channel.h"
#include "User.h"

#include <set>
#include <string>
#include <unordered_map>

#include <boost/asio.hpp>

class Server: public boost::enable_shared_from_this<Server>
{
    std::set<boost::shared_ptr<User>> users;
    std::unordered_map<std::string, boost::shared_ptr<Channel>> channels;

    boost::asio::io_service& io_service;
    boost::asio::ip::tcp::acceptor acceptor;

    std::string servername;

    void listen_for_next_user();

public:
    Server(const std::string &servername, boost::asio::io_service &io_service, const boost::asio::ip::tcp::endpoint &endpoint);

    void handle_accept(boost::shared_ptr<User> user, const boost::system::error_code& error);

    const std::string& get_servername() const
    {
        return servername;
    }

    boost::shared_ptr<User> get_user(const std::string &nickname) const;
    boost::shared_ptr<Channel> get_channel(const std::string &roomname) const;

    void add_user(boost::shared_ptr<User> user);
    void remove_user(boost::shared_ptr<User> user);

    void add_channel(boost::shared_ptr<Channel> channel);
    void remove_channel(boost::shared_ptr<Channel> channel);
};
