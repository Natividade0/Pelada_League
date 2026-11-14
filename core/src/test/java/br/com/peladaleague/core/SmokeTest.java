package br.com.peladaleague.core;
import java.util.*;
public final class SmokeTest {
 public static void main(String[] args){
  List<TeamBalancer.Player> players=new ArrayList<>();
  for(int i=0;i<14;i++)players.add(new TeamBalancer.Player(i,"P"+i,i<3?"GOL":"LIN",70+i));
  var draw=TeamBalancer.draw(players,true,new Random(3));
  if(draw.a().size()!=7||draw.b().size()!=7)throw new AssertionError("Tamanho incorreto");
  var multi=TeamBalancer.drawTeams(players,3,true,new Random(9));
  Set<Integer> ids=new HashSet<>();for(var group:multi.teams()){if(group.size()<4)throw new AssertionError("Time pequeno");for(var p:group)if(!ids.add(p.id()))throw new AssertionError("Duplicados");}
  if(ids.size()!=14)throw new AssertionError("Jogadores perdidos");
  if(AttendanceRules.eligible(AttendanceRules.Status.PENDING)||AttendanceRules.eligible(AttendanceRules.Status.WAITLIST)||!AttendanceRules.eligible(AttendanceRules.Status.CONFIRMED))throw new AssertionError("Sorteio com ausente");
  if(AttendanceRules.decide(AttendanceRules.Status.PENDING,AttendanceRules.Status.CONFIRMED,10,10)!=AttendanceRules.Status.WAITLIST)throw new AssertionError("Lista de espera");
  if(!AttendanceRules.lineupValid(List.of(1,2,3,4),List.of(List.of(1,2),List.of(3,4))))throw new AssertionError("Escalação válida rejeitada");
  if(AttendanceRules.lineupValid(List.of(1,2,3,4),List.of(List.of(1,2),List.of(3,5))))throw new AssertionError("Ausente escalado");
  if(AttendanceRules.lineupValid(List.of(1,2,3,4),List.of(List.of(1,2),List.of(1,4))))throw new AssertionError("Duplicado escalado");
  var table=LeagueRules.standings(List.of("A","B"),List.of(new LeagueRules.Result("A","B",4,3,true),new LeagueRules.Result("B","A",20,0,false)));
  if(table.get(0).points!=3||table.get(0).goalsFor!=4)throw new AssertionError("Pendente entrou na tabela");
  if(!LeagueRules.scoreConsistent(4,3,3,2,1,1))throw new AssertionError("Contagem de gols contra");
  System.out.println("SmokeTest OK: sorteio 2/3 times, elegibilidade, lista de espera, súmula e classificação");
 }
}
