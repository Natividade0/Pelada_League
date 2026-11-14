package br.com.peladaleague.core;
import java.util.*;
public final class RegressionTest {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){int draws=0;for(int n:new int[]{8,14,24,40})for(int k=2;k<=4;k++)for(boolean balanced:new boolean[]{false,true})for(int seed=0;seed<20;seed++){
        List<TeamBalancer.Player> players=new ArrayList<>();for(int i=0;i<n;i++)players.add(new TeamBalancer.Player(i,"P"+i,i<k?"GOL":new String[]{"ZAG","MEI","ATA"}[i%3],30+(i*13)%70));
        var draw=TeamBalancer.drawTeams(players,k,balanced,new Random(seed));Set<Integer> ids=new HashSet<>();int min=100,max=0;for(var team:draw.teams()){min=Math.min(min,team.size());max=Math.max(max,team.size());int gk=0;for(var p:team){check(ids.add(p.id()),"Duplicate player");if(p.position().equals("GOL"))gk++;}if(balanced)check(gk==1,"Unbalanced goalkeepers");}check(ids.size()==n,"Missing player");check(max-min<=1,"Unbalanced team sizes");draws++;
    }
    var table=LeagueRules.standings(List.of("A","B","C"),List.of(new LeagueRules.Result("A","B",3,1,true),new LeagueRules.Result("B","C",2,2,true),new LeagueRules.Result("C","A",99,0,false)));
    check(table.get(0).name.equals("A")&&table.get(0).points==3,"Winner or points wrong");check(table.get(0).played==1,"Provisional result counted");
    try{TeamBalancer.drawTeams(List.of(new TeamBalancer.Player(1,"A","GOL",80),new TeamBalancer.Player(1,"A","GOL",80),new TeamBalancer.Player(2,"B","ATA",80),new TeamBalancer.Player(3,"C","ATA",80)),2,true,new Random());throw new AssertionError("Duplicate accepted");}catch(IllegalArgumentException expected){}
    System.out.println("RegressionTest OK: "+draws+" draws, 2–4 teams, positions/keepers, no duplicates, standings exclude provisional results");}
}
