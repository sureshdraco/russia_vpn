#! /usr/bin/perl
##
## vi:ts=4
##
##---------------------------------------------------------------------------##
##
##  Author:
##      Markus F.X.J. Oberhumer         <markus@oberhumer.com>
##
##  Description:
##      Remove timing values from a table created by table.pl
##
##  Copyright (C) 1996-2011 Markus Franz Xaver Johannes Oberhumer
##
##---------------------------------------------------------------------------##


while (<>) {
    if (substr($_,56) =~ /^\s+[\d\.]+\s+[\d\.]+\s+\|\s*\n$/) {
        substr($_,56) = "     0.000     0.000 |\n";
    }
    print;
}

exit(0);

