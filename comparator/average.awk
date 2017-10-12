{
sum[$5]+=$2
    cnt[$5]++
}


END {
    if ("cpu" in sum) {
       print "Name" "\t" "Total Energy" "\t" "Count" "\t" "Average Energy" "\t" "Energy Used vs CPU"
    } else {
       print "Name" "\t" "Total Energy" "\t" "Count" "\t" "Average Energy" 
    }   
    for (i in sum) {
        if ("cpu" in sum) {
          print i "\t" sum[i] "\t" cnt[i] "\t" sum[i]/cnt[i] "\t" ((sum[i]/cnt[i])/(sum["cpu"]/cnt["cpu"]))
        } else {
          print i "\t" sum[i] "\t" cnt[i] "\t" sum[i]/cnt[i]
        }
    }  

}
