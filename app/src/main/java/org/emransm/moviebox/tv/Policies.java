package org.emransm.moviebox.tv;

import java.util.*;

/** Small platform-independent policies shared by UI and tests. */
public final class Policies {
    private Policies() {}
    public static int[] episodes(String explicit,int maximum) {
        TreeSet<Integer> numbers=new TreeSet<>();
        if(explicit!=null&&!explicit.trim().isEmpty()) {
            for(String value:explicit.split(","))try{int n=Integer.parseInt(value.trim());if(n>0&&n<=10000)numbers.add(n);}catch(NumberFormatException ignored){}
        }else for(int n=1;n<=Math.min(10000,Math.max(0,maximum));n++)numbers.add(n);
        int[] result=new int[numbers.size()];int index=0;for(int value:numbers)result[index++]=value;return result;
    }
    public static long seekPosition(long target,long duration) {
        if(duration<=0)return -1;
        return Math.max(0,Math.min(target,Math.max(0,duration-250)));
    }
    public static boolean newer(String candidate,String current) {
        if(candidate==null||current==null)return false;
        candidate=candidate.replaceFirst("^v","");current=current.replaceFirst("^v","");
        if(!candidate.matches("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})")||!current.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))return false;
        String[] a=candidate.split("\\."),b=current.split("\\.");
        for(int i=0;i<3;i++){int compare=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(compare!=0)return compare>0;}return false;
    }
    public static boolean eligibleResource(int type,int wantedSeason,int wantedEpisode,int season,int episode,int membership,boolean locked,int quality,long size,long duration) {
        return membership==0&&!locked&&quality>0&&quality<=2160&&size>=0&&duration>=0
            &&!(duration>0&&size>0&&size<3145728)
            &&(type!=2||(wantedSeason>0&&wantedEpisode>0&&wantedSeason==season&&wantedEpisode==episode));
    }
}
