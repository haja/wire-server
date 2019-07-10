#!/bin/sh

sudo docker exec -it demo_wire_smtp /bin/sh -c "grep -FrIB1 \"verification\" /var/spool/exim4"
