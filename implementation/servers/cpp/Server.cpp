#include "Server.h"

#include <iostream>

#include <boost/make_shared.hpp>

Server::Server(const std::string &servername, boost::asio::io_service &io_service, const boost::asio::ip::tcp::endpoint &endpoint):
        io_service(io_service), acceptor(io_service, endpoint), servername{servername} {
    listen_for_next_user();
}

void Server::listen_for_next_user() {
    auto new_user = boost::make_shared<User>(io_service, this);
    acceptor.async_accept(
            new_user -> get_socket(),
            boost::bind(
                    &Server::handle_accept,
                    this,
                    new_user,
                    boost::asio::placeholders::error));

}

void Server::handle_accept(boost::shared_ptr<User> user, const boost::system::error_code &error) {
    if(!error) {
        std::cout << "Got a new connecting guy!" << std::endl;

        user -> start();
        add_user(user);

        // Create another user
        listen_for_next_user();
    } else {
        std::cerr << "Got an error: " << error << std::endl;
    }
}

void Server::add_user(boost::shared_ptr<User> user) {
    users.insert(user);
}

void Server::remove_user(boost::shared_ptr<User> user) {
    users.erase(user);
}

boost::shared_ptr<User> Server::get_user(const std::string &nickname) const {
    for(auto& it: users){
        if(it -> get_name() == nickname)
            return it;
    }

    return boost::shared_ptr<User>(nullptr);
}

boost::shared_ptr<Channel> Server::get_channel(const std::string &channel_name) const {
    auto it = channels.find(channel_name);

    if (it != channels.end()) {
        return it -> second;
    }

    return boost::shared_ptr<Channel>(nullptr);
}

void Server::add_channel(boost::shared_ptr<Channel> channel) {
    channels.insert({channel -> get_name(), channel});
}

void Server::remove_channel(boost::shared_ptr<Channel> channel) {
    channels.erase(channel -> get_name());
}
