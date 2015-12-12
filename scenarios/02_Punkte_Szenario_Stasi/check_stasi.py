#!/usr/bin/env python
import socket
import string
import sys
import random
import re


'''
This is merely an example script which verifies the scenario in which intercept
messages are forwarded to a given user (the username of Agent Grabowski). As
per the scenario description, other means of interception are allowed as well,
be creative.
'''


# you may give this script another port and host to check over argv like this:
#     ./check_whois.py 9999 otherhost.com
if len(sys.argv) < 2:
    print 'Usage: %s <username> [PORT] [HOST]'
    sys.exit(0)

PORT = 6667 if len(sys.argv) < 3 else sys.argv[2]
HOST = 'localhost' if len(sys.argv) < 3 else sys.argv[3]


def generate_string(length):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


class RandomClient:
    def __init__(self, nickname=None):
        self.nickname = generate_string(10) if not nickname else nickname
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

spy = RandomClient(sys.argv[1])
agent1 = RandomClient('definitelynotaspy')
agent2 = RandomClient()

channel = '#topsecret'
agent1.send('JOIN :%s' % channel)
agent2.send('JOIN :%s' % channel)
spy.send('JOIN :%s' % channel)

agent2.send('PRIVMSG %s :%s' % (agent1.nickname, 'Top Secret'))

spy_reply, agent_reply = spy.recv_until('PRIVMSG', error='Waited for 5 seconds on PRIVMSG for user %s.' % spy.nickname), \
                         agent1.recv_until('PRIVMSG', error='Waited for 5 seconds on PRIVMSG for user %s.' % agent1.nickname)

privmsg_spy, privmsg_agent = spy_reply.split('\n')[::-1][1], agent_reply.split('\n')[::-1][1]
privmsg_pattern = r':%s!\w+@\w+ PRIVMSG \w+ :Top Secret' % agent2.nickname

assert re.match(privmsg_pattern, privmsg_spy), \
       'User %s did not receive correct PRIVMSG, incomplete backdoor.' % spy.nickname

assert re.match(privmsg_pattern, privmsg_agent), \
       'User %s did not receive correct PRIVMSG' % agent1.nickname

print 'You made it. :)'
