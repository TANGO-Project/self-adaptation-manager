{
sum[$5]+=$4
    cnt[$5]++
}


END {
    print "Name" "\t" "Total Energy" "\t" "Count" "\t" "Average Energy"
    for (i in sum)
        print i "\t" sum[i] "\t" cnt[i] "\t" sum[i]/cnt[i]

}
