#include <iostream>

#include "Server.h"
#include <boost/asio.hpp>

int main(int argc, char **argv)
{
    boost::asio::io_service iosrv;
    boost::asio::ip::tcp::endpoint endpoint(boost::asio::ip::tcp::v4(), 6667);

    try {
        Server srv("FluxServ", iosrv, endpoint);
        iosrv.run();
    } catch(boost::system::system_error const& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
    return 0;
}
