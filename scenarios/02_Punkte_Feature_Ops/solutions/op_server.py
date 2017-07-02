#!/usr/bin/env python3
"""Asynchronous FluxChat implementation

UPDATE: This implements the OP functionality.

You don't have to understand the asyncio module of Python very well to be able
to work with it. What you need to know is this: Everything runs in a single
thread, so there are no race conditions. You basically can do almost everything
you want to _except_ have a long-running loop somewhere. We are speaking about
"some seconds" long running here. Anything else won't be noticeable in our lab.
So: Have fun hacking on this and ask whenever you feel you need help :-)

Needs at least Python 3.4 to work.
"""
import asyncio
import socket
import sys


# name of the server - change this to anything (without spaces)
SERVER_NAME = 'fluxserver'
# character encoding of network messages (changing this should not be required)
ENCODING = 'utf-8'


# global list holding all connected clients
_clients = []


def list_channel(channel):
    """List all clients in channel"""
    return [client for client in _clients if channel in client.channels]


def get_client(nickname):
    """Return the first client with the specified nickname"""
    for client in _clients:
        if client.nickname == nickname:
            return client
    return None


class FluxChatProtocol(asyncio.Protocol):
    def connection_made(self, client_socket):
        """Initialize internal variables after this client has connected"""
        self.ip, self.port = client_socket.get_extra_info('peername')[:2]
        self.nickname = None
        self.hostname = socket.gethostbyaddr(self.ip)[0]
        self.channels = {}
        print('Connection from %s:%d' % (self.ip, self.port))
        self._socket = client_socket
        self._buffer = ''
        self._handlers = {
            'JOIN': self.handle_join,
            'NICK': self.handle_nick,
            'PART': self.handle_part,
            'PRIVMSG': self.handle_privmsg,
            'QUIT': self.handle_quit,
            'KICK': self.handle_kick,
        }
        _clients.append(self)

    def connection_lost(self, _):
        """Tell other clients after this client has disconnected"""
        if self in _clients:
            self.handle_quit()

    def data_received(self, data):
        """Parse and execute command after data has been received"""
        self._buffer += data.decode(ENCODING)
        if '\n' not in self._buffer:
            # no command in the buffer yet, since all commands end with \n
            return
        lines = self._buffer.split('\n')
        # put everything after the last \n back into self._buffer
        self._buffer = lines[-1]
        # everything else should be complete commands
        for line in lines[:-1]:
            command, parameters = self.parse(line)
            print('Received command: %s %s' % (command, parameters))
            if self.nickname is None and command != 'NICK':
                # clients have to send a valid NICK command first
                continue
            if command in self._handlers:
                # commands will receive protocol params as function params
                self._handlers[command](*parameters)
            else:
                self.send_error(421, command, 'Unknown command')

    def parse(self, line):
        """Parse a command and return its name + parameters"""
        # cut whitespace left and right
        line = line.strip()
        # if there is no space, there are no parameters
        if ' ' not in line:
            return line.upper(), []
        # split at the first space to get the command (e.g. JOIN<break>:#chan)
        command, line_rest = line.split(' ', 1)
        # this loop checks if we have reached the last parameter (denoted by a
        # colon at the start of the last parameter) and if not, keeps adding
        # to the parameter list
        parameters = []
        while not line_rest.startswith(':') and ' ' in line_rest:
            parameter, line_rest = line_rest.split(' ', 1)
            parameters.append(parameter)
        # cut away the leading colon (:) of the last parameter if necessary
        parameter = line_rest[1:] if line_rest.startswith(':') else line_rest
        parameters.append(parameter)
        return command.upper(), parameters

    def identify(self):
        """Build a nickname@hostname identifier"""
        return '%s!x@%s' % (self.nickname, self.hostname)

    def send(self, data):
        """Send data to the connected client and encode it if necessary"""
        if not isinstance(data, bytes):
            data = data.encode(ENCODING)
        self._socket.write(data + b'\n')

    def send_error(self, code, param, msg):
        """Build and send an error message"""
        self.send(':%s %d %s %s :%s'
                  % (SERVER_NAME, code, self.nickname, param, msg))

    def send_error2(self, code, param, msg):
        self.send(':%s %d %s :%s'
                  % (SERVER_NAME, code, param, msg))

    def handle_join(self, channel):
        """Join a channel"""
        if not channel.startswith('#') or ' ' in channel or len(channel) == 0:
            self.send_error(479, channel, 'Illegal channel name')
        if channel in self.channels:
            return
        users_in_channel = list_channel(channel)
        self.channels[channel] = len(users_in_channel) == 0

        users = []
        for client in list_channel(channel):
            # build a list of users in the channel
            prefix = '@' if client.channels[channel] else ''
            users.append(prefix + client.nickname)
            # notify all clients of the new user
            client.send(':%s JOIN :%s' % (self.identify(), channel))

        # tell the new client which users are in this channel
        user_list = ' '.join(users)
        reply_list = '%s @ %s :%s' % (self.nickname, channel, user_list)
        reply_eol = '%s %s :End of /NAMES list.' % (self.nickname, channel)

        self.send(':%s %03d %s' % (SERVER_NAME, 353, reply_list))
        self.send(':%s %03d %s' % (SERVER_NAME, 366, reply_eol))

    def handle_nick(self, nickname):
        """Change the nickname"""
        if (nickname.startswith('#') or ' ' in nickname or '@' in nickname or
                '!' in nickname or len(nickname) == 0):
            self.send_error(432, nickname, 'Erroneus nickname')
            return
        if get_client(nickname):
            self.send_error(433, nickname, 'Nickname is already in use')
            return
        # notify all clients in channels where this client is present
        msg = ':%s NICK :%s' % (self.identify(), nickname)
        self.send(msg)
        for channel in self.channels:
            for client in list_channel(channel):
                if client != self:
                    client.send(msg)
        self.nickname = nickname

    def handle_part(self, channel, message=''):
        """Leave a channel"""
        if channel not in self.channels:
            return
        for client in list_channel(channel):
            client.send(':%s PART :%s' % (self.identify(), channel))
        self.channels.pop(channel)

    def handle_privmsg(self, dest, msg):
        """Send a message to a channel or a user"""
        command = ':%s PRIVMSG %s :%s' % (self.identify(), dest, msg)
        if dest.startswith('#'):
            for client in list_channel(dest):
                if client != self:
                    client.send(command)
        else:
            # message to another client
            peer = get_client(dest)
            if peer is None:
                self.send_error(401, dest, 'No such nick/channel')
                return
            peer.send(command)

    def handle_quit(self, message=''):
        """Disconnect from the server"""
        # remove from client list
        _clients.remove(self)
        # leave all channels
        for channel in self.channels.copy():
            self.handle_part(channel)
        # close connection
        self._socket.close()

    def handle_kick(self, channel, nick, message=''):
        target = get_client(nick)
        if channel not in self.channels:
            self.send_error(442, channel, "You're not on that channel")
            return
        if not self.channels[channel]:
            self.send_error(482, channel, "You're not channel operator")
            return
        if target is None or channel not in target.channels:
            self.send_error(441, '%s %s' % (nick, channel),
                            "They aren't on that channel")
            return
        for client in list_channel(channel):
            client.send(':%s KICK %s %s :%s'
                        % (self.identify(), channel, target.nickname, message))
        target.channels.pop(channel)

if __name__ == '__main__':
    # parse the command line and accept 2 optional parameters
    if '--help' in sys.argv:
        print('Usage: %s [PORT [HOST]]' % sys.argv[0])
        sys.exit()
    PORT = 6667 if len(sys.argv) < 2 else int(sys.argv[1])
    HOST = 'localhost' if len(sys.argv) < 3 else sys.argv[2]

    # prepare an asynchrous loop, create a server instance and start everything
    # docs at https://docs.python.org/3.4/library/asyncio-protocol.html
    loop = asyncio.get_event_loop()
    coro = loop.create_server(FluxChatProtocol, HOST, PORT)
    server = loop.run_until_complete(coro)
    print('Starting on %s:%d' % (HOST, PORT))
    print('Press Ctrl+C to stop the server')
    try:
        loop.run_forever()
    except KeyboardInterrupt:
        print('Server stopping...')
    finally:
        server.close()
        loop.run_until_complete(server.wait_closed())
        loop.close()
