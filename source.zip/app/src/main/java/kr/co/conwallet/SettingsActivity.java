package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int EXPORT_JSON = 201;
    private static final int IMPORT_JSON = 202;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("콘지갑 설정");
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,28)); root.setBackgroundColor(Ui.colorBg()); scroll.addView(root);
        TextView title = Ui.text(this,"설정",28,Ui.colorText()); title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD); root.addView(title);
        TextView intro = Ui.text(this,"로그인·서버·광고 없이 이 기기 안에만 저장합니다.",13,Ui.colorSecondary()); root.addView(intro);

        root.addView(section("만료 알림"));
        root.addView(toggle("D-30 오전 9시", NotificationPrefs.D30));
        root.addView(toggle("D-7 오전 9시", NotificationPrefs.D7));
        root.addView(toggle("D-1 오전 9시", NotificationPrefs.D1));
        TextView notice = Ui.text(this,"Android의 배터리 최적화 상태에 따라 알림 시각이 조금 늦어질 수 있습니다.",12,Ui.colorSecondary()); root.addView(notice);
        Button reschedule = new Button(this); reschedule.setText("현재 만료 알림 다시 예약"); reschedule.setOnClickListener(v->{NotificationHelper.rescheduleAll(this); Toast.makeText(this,"알림을 다시 예약했습니다.",Toast.LENGTH_SHORT).show();}); root.addView(reschedule);

        root.addView(section("백업 · 복원"));
        TextView compatible = Ui.text(this,"iPhone 콘지갑과 같은 JSON 백업 형식을 사용합니다. 이미지도 백업 파일 안에 포함됩니다.",13,Ui.colorSecondary()); root.addView(compatible);
        Button export = new Button(this); export.setText("JSON 백업 내보내기"); export.setOnClickListener(v->exportBackup()); root.addView(export);
        Button restore = new Button(this); restore.setText("JSON 백업 불러오기"); restore.setOnClickListener(v->importBackup()); root.addView(restore);

        root.addView(section("데이터 관리"));
        Button deleteAll = new Button(this); deleteAll.setText("모든 기프티콘 삭제"); deleteAll.setTextColor(Color.rgb(180,35,35)); deleteAll.setOnClickListener(v->confirmDeleteAll()); root.addView(deleteAll);
        return scroll;
    }

    private TextView section(String s) {
        TextView t=Ui.text(this,s,17,Ui.colorText()); t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.topMargin=Ui.dp(this,22); lp.bottomMargin=Ui.dp(this,6); t.setLayoutParams(lp); return t;
    }

    private Switch toggle(String label, String key) {
        Switch sw=new Switch(this); sw.setText(label); sw.setChecked(NotificationPrefs.get(this,key));
        sw.setOnCheckedChangeListener((button,checked)->{NotificationPrefs.set(this,key,checked); NotificationHelper.rescheduleAll(this);});
        return sw;
    }

    private void exportBackup() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/json");
        String stamp=new SimpleDateFormat("yyyyMMdd-HHmm",Locale.KOREA).format(new Date()); i.putExtra(Intent.EXTRA_TITLE,"ConWallet-backup-"+stamp+".json"); startActivityForResult(i,EXPORT_JSON);
    }

    private void importBackup() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/json"); startActivityForResult(i,IMPORT_JSON);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return; Uri uri=data.getData();
        if(requestCode==EXPORT_JSON){
            try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new Exception("파일을 열 수 없습니다."); out.write(BackupService.exportJson(this)); Toast.makeText(this,"백업을 저장했습니다.",Toast.LENGTH_LONG).show();}
            catch(Exception e){Toast.makeText(this,"백업 실패: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        } else if(requestCode==IMPORT_JSON){
            new AlertDialog.Builder(this).setTitle("백업 불러오기").setMessage("같은 ID의 기프티콘은 백업 내용으로 업데이트됩니다. 계속할까요?")
                    .setNegativeButton("취소",null).setPositiveButton("불러오기",(d,w)->{
                        try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new Exception("파일을 열 수 없습니다."); int count=BackupService.importJson(this,readAll(in)); Toast.makeText(this,count+"개 기프티콘을 불러왔습니다.",Toast.LENGTH_LONG).show();}
                        catch(Exception e){Toast.makeText(this,"복원 실패: "+e.getMessage(),Toast.LENGTH_LONG).show();}
                    }).show();
        }
    }

    private byte[] readAll(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();}

    private void confirmDeleteAll(){
        new AlertDialog.Builder(this).setTitle("모든 데이터 삭제").setMessage("저장된 모든 기프티콘과 이미지를 삭제합니다. 되돌릴 수 없습니다.")
                .setNegativeButton("취소",null).setPositiveButton("모두 삭제",(d,w)->{
                    for(Gifticon g:GifticonDb.get(this).all()){NotificationHelper.cancel(this,g.id);ImageStore.delete(g.imagePath);} GifticonDb.get(this).deleteAll(); Toast.makeText(this,"모두 삭제했습니다.",Toast.LENGTH_SHORT).show();
                }).show();
    }
}
