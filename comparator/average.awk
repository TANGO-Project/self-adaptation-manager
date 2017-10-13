{
sum[$5]+=$2
    cnt[$5]++
    time[$5]+=$3
}


END {
    if ("cpu" in sum) {
       print "Name" "\t" "Count" "\t" "Total Energy" "\t" "Average Energy" "\t" "Total Time" "\t" "Average Time" "\t" "Energy Used vs CPU" "\t" "Duration vs CPU"
    } else {
       print "Name" "\t" "Count" "\t" "Total Energy" "\t" "Average Energy" "\t" "Total Time" "\t" "Average Time"
    }   
    for (i in sum) {
        if ("cpu" in sum) {
          print i "\t" cnt[i] "\t" sum[i] "\t" sum[i]/cnt[i] "\t" time[i] "\t" time[i]/cnt[i] "\t" ((sum[i]/cnt[i])/(sum["cpu"]/cnt["cpu"])) "\t" ((time[i]/cnt[i])/(time["cpu"]/cnt["cpu"]))
        } else {
          print i "\t" cnt[i] "\t" sum[i] "\t" sum[i]/cnt[i] "\t" time[i] "\t" time[i]/cnt[i]
        }
    }  

}
