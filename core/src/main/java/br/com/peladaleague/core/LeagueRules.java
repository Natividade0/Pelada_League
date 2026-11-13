package br.com.peladaleague.core;

import java.util.*;

/** Pure, testable league calculations. Only validated matches belong in an official table. */
public final class LeagueRules {
    private LeagueRules() {}
    public record Result(String teamA, String teamB, int goalsA, int goalsB, boolean approved) {}
    public static final class Standing {
        public final String name;
        public int played, wins, draws, losses, goalsFor, goalsAgainst, points;
        public Standing(String name) { this.name=name; }
        public int goalDifference(){return goalsFor-goalsAgainst;}
    }
    public static List<Standing> standings(Collection<String> teams, Collection<Result> results){
        Map<String,Standing> map=new LinkedHashMap<>();
        for(String team:teams) map.put(team,new Standing(team));
        for(Result r:results){
            if(!r.approved||r.goalsA<0||r.goalsB<0||r.teamA.equals(r.teamB)) continue;
            Standing a=map.get(r.teamA),b=map.get(r.teamB);
            if(a==null||b==null)continue;
            a.played++;b.played++;a.goalsFor+=r.goalsA;a.goalsAgainst+=r.goalsB;
            b.goalsFor+=r.goalsB;b.goalsAgainst+=r.goalsA;
            if(r.goalsA>r.goalsB){a.wins++;a.points+=3;b.losses++;}
            else if(r.goalsA<r.goalsB){b.wins++;b.points+=3;a.losses++;}
            else {a.draws++;b.draws++;a.points++;b.points++;}
        }
        List<Standing> sorted=new ArrayList<>(map.values());
        sorted.sort(Comparator.comparingInt((Standing s)->s.points).reversed()
                .thenComparing(Comparator.comparingInt((Standing s)->s.wins).reversed())
                .thenComparing(Comparator.comparingInt(Standing::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt((Standing s)->s.goalsFor).reversed())
                .thenComparing(s->s.name));
        return sorted;
    }
    public static boolean scoreConsistent(int scoreA,int scoreB,int regularA,int regularB,int ownAForB,int ownBForA){
        return scoreA>=0&&scoreB>=0&&scoreA==regularA+ownBForA&&scoreB==regularB+ownAForB;
    }
}
