#!/usr/bin/env python3
"""Check if you have correctly implemented the WHOIS feature
Call like this:
    $ ./check_whois.py
Or, if you want to change the port and host:
    $ ./check_whois.py PORT HOST
"""
import sys


# close your eyes - if you don't look it does not look hacky :-)
sys.path.append('../../implementation/checkers')
import checker


PORT = 6667 if len(sys.argv) < 2 else sys.argv[1]
HOST = 'localhost' if len(sys.argv) < 3 else sys.argv[2]


client1 = checker.RandomClient(HOST, PORT)
client2 = checker.RandomClient(HOST, PORT)


channel = '#' + checker.generate_string(10)
client1.send('JOIN :%s' % channel)


client2.send('WHOIS :%s' % client1.nickname)
error_msg = 'Waited for 5 seconds on 318 status code.'
replies = client2.recv_until(' 318 ', error=error_msg)


checker.check_regex(r':[^ ]+ 311 %s %s x [^ ]+ \* :[\da-fA-F:\.]+:\d+'
                    % (client2.nickname, client1.nickname),
                    replies,
                    'Status 311 message missing/incomplete')


checker.check_regex(r':[^ ]+ 319 %s %s :%s'
                    % (client2.nickname, client1.nickname, channel),
                    replies,
                    'Status 319 message mssing/incomplete')


checker.check_regex(r':[^ ]+ 318 %s %s :End of /WHOIS list\.'
                    % (client2.nickname, client1.nickname),
                    replies,
                    'Status 318 message incomplete')


unconnected_nick = checker.generate_string(20)
client2.send('WHOIS :%s' % unconnected_nick)
replies = client2.recv_until(' 401 ',
                             error='Waited for 5 seconds on 401 status code.')
checker.check_regex(r':[^ ]+ 401 %s %s :No such nick/channel'
                    % (client2.nickname, unconnected_nick),
                    replies,
                    'Status 401 message incomplete')
