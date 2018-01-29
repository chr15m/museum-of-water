# Museum of Water
/usr/local/bin/hostapd -B /etc/hostapd.conf
/sbin/ifconfig wlan0 192.168.42.1
/usr/sbin/udhcpd /etc/udhcpd.conf
cd ~tc/MoW && ./serve &

