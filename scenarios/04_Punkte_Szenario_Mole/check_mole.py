#!/usr/bin/env python3


import re
import sys
import time
import socket
import string
import random


def generate_string(length):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


class Mole:
    def __init__(self, host, port, channel):
        pre = generate_string(random.randint(2, 5))
        post = generate_string(random.randint(2, 5))

        self.nickname = '%smole%s' % (pre, post)
        self.channel = channel

        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.settimeout(5)
        self._socket.connect((host, port))

    def hunt_down(self):
        self.send('NICK :%s' % self.nickname)
        self.send('JOIN :%s' % self.channel)

        needle = ':End of /NAMES'
        reply = self.recv_until(needle, error='Did not receive a user list'\
                                ' after waiting 5 seconds.')

        for line in reply.split('\n'):
            assert len(line.split()) >= 3, 'Failed to parse server response.'

            _, code, *tokens = line.split()
            if code.lower() != '353':
                continue

            users = [t.replace(':', '').
                       replace('+', '').
                       replace('@', '') for t in tokens[3:]]
            return self.nickname in users

    def send(self, cmd):
        self._socket.send(cmd.encode() + b'\n')

    def recv_until(self, text, error):
        data = ''
        try:
            while text not in data:
                data += self._socket.recv(1024).decode()
        except socket.timeout:
            raise Exception(error)
        return data


def main():
    port = 6667 if len(sys.argv) < 3 else sys.argv[1]
    host = 'localhost' if len(sys.argv) < 3 else sys.argv[2]

    mole = Mole(host, port, '#fun')
    if mole.hunt_down():
        print('Failed to hide the mole in the user list.')
    else:
        print('Test succeeded!')


if __name__ == '__main__':
    main()

