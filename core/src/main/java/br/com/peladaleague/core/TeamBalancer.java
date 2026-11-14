package br.com.peladaleague.core;
import java.util.*;
/** Stable, testable balanced draw for two to six teams. Requires attendance filtering by caller. */
public final class TeamBalancer {
    public record Player(int id,String name,String position,int skill) {}
    public record Draw(List<Player> a,List<Player> b,int difference) {}
    public record MultiDraw(List<List<Player>> teams,int imbalance) {}
    private TeamBalancer(){}
    public static Draw draw(List<Player> selected,boolean balanced,Random random){
        if(selected.size()<2)throw new IllegalArgumentException("Pelo menos dois jogadores");
        MultiDraw d=drawTeams(selected,2,balanced,random);
        return new Draw(d.teams.get(0),d.teams.get(1),d.imbalance);
    }
    public static MultiDraw drawTeams(List<Player> players,int teams,boolean balanced,Random rng){
        if(teams<2||teams>6)throw new IllegalArgumentException("Times entre 2 e 6");
        if(players.size()<teams*2)throw new IllegalArgumentException("Pelo menos dois jogadores por time");
        Set<Integer> unique=new HashSet<>();for(Player p:players)if(!unique.add(p.id()))throw new IllegalArgumentException("Jogador duplicado");
        MultiDraw best=null;int tries=balanced?800:1;
        int[] sizes=new int[teams];for(int i=0;i<players.size();i++)sizes[i%teams]++;
        for(int trial=0;trial<tries;trial++){
            List<Player> shuffled=new ArrayList<>(players);Collections.shuffle(shuffled,rng);
            List<List<Player>> squads=new ArrayList<>();int k=0;
            for(int t=0;t<teams;t++){List<Player> squad=new ArrayList<>();for(int j=0;j<sizes[t];j++)squad.add(shuffled.get(k++));squads.add(squad);}
            int metric=imbalance(squads);
            if(best==null||metric<best.imbalance)best=new MultiDraw(squads,metric);
            if(metric==0)break;
        }
        return best;
    }
    private static int positionPenalty(List<List<Player>> teams){
        int penalty=0;for(String pos:List.of("ZAG","MEI","ATA")){int min=999,max=0;for(List<Player> team:teams){int count=0;for(Player p:team)if(pos.equals(p.position()))count++;min=Math.min(min,count);max=Math.max(max,count);}penalty+=(max-min)*15;}return penalty;
    }
    private static int imbalance(List<List<Player>> teams){
        int min=999,max=0,keepersMin=999,keepersMax=0;
        for(List<Player> squad:teams){int total=0,keepers=0;for(Player p:squad){total+=p.skill();if("GOL".equals(p.position()))keepers++;}
            int avg=total*100/squad.size();min=Math.min(min,avg);max=Math.max(max,avg);keepersMin=Math.min(keepersMin,keepers);keepersMax=Math.max(keepersMax,keepers);
        }
        return max-min+(keepersMax-keepersMin)*10000+positionPenalty(teams);
    }
}
