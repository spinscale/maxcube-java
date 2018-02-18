#!/usr/bin/env bash

echo "*** Use 'sudo raspi-config' to setup overclocking, networking, locale and timezone ***"
echo "*** Use 'passwd' to change default password ***"

sudo systemctl enable ssh
sudo systemctl start ssh
sudo apt-get -y install docker git
