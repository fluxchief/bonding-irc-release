"""Common functionality for scenario checkers

You probably do not have to change anything in here.
"""
import random
import socket
import string
import sys
import re


def generate_string(length):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


def check_regex(regex, input, error_msg, invert=False):
    try:
        match = re.search(regex, input)
        if invert:
            match = not match
        assert match, error_msg
    except AssertionError as error:
        print('I got this from the server:')
        print(input)
        print(error)
        sys.exit(1)


class RandomClient:
    def __init__(self, host, port):
        self.nickname = generate_string(10)
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.settimeout(5)
        self._socket.connect((host, port))
        self.send('NICK :%s' % self.nickname)

    def send(self, cmd):
        self._socket.send(cmd.encode() + b'\n')

    def recv_until(self, text, error):
        data = ''
        try:
            while text not in data:
                data += self._socket.recv(1024).decode()
        except socket.timeout:
            print('Data until now:')
            print(data)
            raise Exception(error)
        return data

