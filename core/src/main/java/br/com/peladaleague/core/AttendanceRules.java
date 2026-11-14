package br.com.peladaleague.core;
import java.util.*;
/** Pure rules, shared by Android and standalone tests. */
public final class AttendanceRules {
    private AttendanceRules(){}
    public enum Status { PENDING, CONFIRMED, WAITLIST, DECLINED }
    public static boolean eligible(Status status){return status==Status.CONFIRMED;}
    public static Status decide(Status current,Status requested,int confirmed,int capacity){
        if(capacity<2||confirmed<0)throw new IllegalArgumentException("Capacidade inválida");
        if(requested==Status.CONFIRMED&&current!=Status.CONFIRMED&&confirmed>=capacity)return Status.WAITLIST;
        return requested;
    }
    public static <T> List<T> confirmed(Collection<T> all,Map<T,Status> statuses){
        List<T> result=new ArrayList<>();for(T item:all)if(eligible(statuses.getOrDefault(item,Status.PENDING)))result.add(item);return result;
    }
    public static boolean lineupValid(Collection<Integer> confirmed,List<? extends Collection<Integer>> squads){
        if(squads.size()<2)return false;Set<Integer> allowed=new HashSet<>(confirmed),used=new HashSet<>();
        for(Collection<Integer> squad:squads){if(squad.size()<2)return false;for(Integer id:squad)if(!allowed.contains(id)||!used.add(id))return false;}
        return true;
    }
}
