package br.com.peladaleague.app;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import java.util.*;

/** Native validated replacement of one player by a confirmed available reserve. */
final class SubstitutionForm extends Screen {
    SubstitutionForm(MainActivity a){super(a);}
    void open(){
        DemoDb.Game game=db.game(a.gameId);
        if(game==null||!"DRAFT".equals(game.status)||!a.canRecord()){
            a.toast("Substituições só podem ser registradas pelo responsável durante a partida em edição.");return;
        }
        LinearLayout form=Ui.col(a);form.setPadding(Ui.dp(a,17),Ui.dp(a,8),Ui.dp(a,17),Ui.dp(a,14));
        Spinner team=spinner(form,"Equipe",Arrays.asList(game.a,game.b));
        Spinner outgoing=spinner(form,"Sai do campo",new ArrayList<>());
        Spinner incoming=spinner(form,"Entra no campo",new ArrayList<>());
        EditText minute=Ui.input(a,"Minuto do jogo");
        minute.setInputType(InputType.TYPE_CLASS_NUMBER);
        minute.setText(""+Math.min(240,a.matchClock.elapsed(game.id)/60000L));
        Ui.put(form,text("Minuto da substituição",12,Ui.WHITE,true),10);
        Ui.put(form,minute,6);
        final List<DemoDb.Player>[] departing=new List[]{new ArrayList<>()};
        final List<DemoDb.Player>[] arriving=new List[]{new ArrayList<>()};
        Runnable update=()->{
            int side=team.getSelectedItemPosition();
            departing[0]=db.roster(game.id,side);
            Set<Long> busy=new HashSet<>();
            for(int t=0;t<2;t++)for(DemoDb.Player p:db.roster(game.id,t))busy.add(p.id);
            arriving[0]=new ArrayList<>();
            for(DemoDb.Player p:db.confirmedPlayers(game.groupId))
                if(!busy.contains(p.id))arriving[0].add(p);
            names(outgoing,departing[0]);
            names(incoming,arriving[0]);
        };
        team.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int index,long id){update.run();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        update.run();
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Registrar substituição").setView(scroll)
            .setNegativeButton("Cancelar",null).setPositiveButton("Registrar",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            try{
                if(departing[0].isEmpty()||arriving[0].isEmpty())
                    throw new IllegalStateException("É necessário ter um jogador confirmado disponível para entrar.");
                int when=Integer.parseInt(minute.getText().toString().trim());
                DemoDb.Player out=departing[0].get(outgoing.getSelectedItemPosition());
                DemoDb.Player in=arriving[0].get(incoming.getSelectedItemPosition());
                db.substitute(game.id,team.getSelectedItemPosition(),out.id,in.id,when);
                dialog.dismiss();a.toast("Substituição registrada.");a.refresh();
            }catch(NumberFormatException e){a.toast("Informe um minuto válido.");}
             catch(IllegalArgumentException|IllegalStateException e){a.toast(e.getMessage());}
        }));
        dialog.show();
    }
    private Spinner spinner(LinearLayout form,String label,List<String> list){
        Ui.put(form,text(label,12,Ui.WHITE,true),10);
        Spinner spinner=new Spinner(a);
        spinner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,list));
        Ui.put(form,spinner,5);
        return spinner;
    }
    private void names(Spinner spinner,List<DemoDb.Player> players){
        List<String> labels=new ArrayList<>();
        for(DemoDb.Player p:players)labels.add(p.name+" · "+p.pos);
        if(labels.isEmpty())labels.add("Nenhum jogador disponível");
        spinner.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));
    }
}
