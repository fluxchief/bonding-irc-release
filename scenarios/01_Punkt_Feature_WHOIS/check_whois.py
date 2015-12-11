#!/usr/bin/env python3
import socket
import string
import sys
import random
import re


# you may give this script another port and host to check over argv like this:
#     ./check_whois.py 9999 otherhost.com
PORT = 6667 if len(sys.argv) < 2 else sys.argv[1]
HOST = 'localhost' if len(sys.argv) < 3 else sys.argv[2]


def generate_string(length):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


class RandomClient:
    def __init__(self):
        self.nickname = generate_string(10)
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.settimeout(5)
        self._socket.connect((HOST, PORT))
        self.send('NICK :%s' % self.nickname)

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


client1 = RandomClient()
client2 = RandomClient()


channel = '#' + generate_string(10)
client1.send('JOIN :%s' % channel)


client2.send('WHOIS :%s' % client1.nickname)
replies = client2.recv_until(' 318 ',
                             error='Waited for 5 seconds on 318 status code.')


assert re.search(r':[^ ]+ 311 %s %s x [^ ]+ \* :[\da-fA-F:\.]+:\d+'
                 % (client2.nickname, client1.nickname),
                 replies), \
       'Status 311 message missing/incomplete'
assert re.search(r':[^ ]+ 319 %s %s :%s'
                 % (client2.nickname, client1.nickname, channel),
                 replies), \
       'Status 319 message mssing/incomplete'
assert re.search(r':[^ ]+ 318 %s %s :End of /WHOIS list\.'
                 % (client2.nickname, client1.nickname),
                 replies), \
       'Status 318 message incomplete'
