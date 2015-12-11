#!/usr/bin/env python3
"""Check if you have correctly implemented the WHOIS feature

Call like this:
    $ ./check_ops.py
Or, if you want to change the port and host:
    $ ./check_ops.py PORT HOST
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
client2.send('JOIN :%s' % channel)


c1r = client1.recv_until(' 366 ', error=('First in channel: Waited for 366 '
                                         'status but got none :('))
c2r = client2.recv_until(' 366 ', error=('Second in channel: Waited for 366 '
                                         'status but got none :('))


checker.check_regex(r':[^ ]+ 353 %s @ %s :@%s'
                    % (client1.nickname, channel, client1.nickname),
                    c1r,
                    'Status 353 message has no OP-prefix for client 1.')

checker.check_regex(r':[^ ]+ 353 %s @ %s :@%s %s'
                    % (client2.nickname, channel, client1.nickname,
                       client2.nickname),
                    c2r,
                    'Status 353 message is broken for second client')


client2.send('KICK %s %s :Test' % (channel, client1.nickname))
client2.recv_until(' 482 %s %s :You\'re not channel operator'
                   % (client2.nickname, channel),
                   error='Youre not channel operator message wasnt sent')


client1.send('KICK %s %s :HAHA' % (channel, client2.nickname))
client1.send('PRIVMSG %s :Writey' % channel)
client2.send('JOIN #%s' % checker.generate_string(21))
resp = client2.recv_until(' 366 ', error='Client 2 does not receive stuff')
checker.check_regex(r':%s!x@[^ ]+ KICK %s %s :HAHA'
                    % (client1.nickname, channel, client2.nickname),
                    resp,
                    'KICK message sent to client2 broken')
checker.check_regex(r'Writey', resp, 'Client2 can still see messages',
                    invert=True)
