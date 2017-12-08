package net.zjy.cardinfo;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private CommonCard commonCard;
    private TextView mTextMessage;
    private ImageView mImageView;
    private TextView mVerboseInfo;
    private TextView mBasicInfo;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    IsoDep.class.getName()
            }
    };

    private NfcAdapter nfcAdapter;
    private PendingIntent nfcIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextMessage = (TextView) findViewById(R.id.message);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mVerboseInfo = (TextView) findViewById(R.id.verboseInfo);
        mVerboseInfo.setMovementMethod(new ScrollingMovementMethod());
        mBasicInfo = (TextView) findViewById(R.id.basicInfo);
        mBasicInfo.setMovementMethod(new ScrollingMovementMethod());
        //verbose = "";
        commonCard = new CommonCard(mVerboseInfo, this);
        /*
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);*/

    }

    private void hideTip() {
        mTextMessage.setVisibility(View.INVISIBLE);
        mImageView.setVisibility(View.INVISIBLE);
        mVerboseInfo.setVisibility(View.VISIBLE);
    }

    private void showTip() {
        mTextMessage.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.VISIBLE);
        mVerboseInfo.setVisibility(View.INVISIBLE);
    }

    protected void onResume() {
        super.onResume();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.no_nfc), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.disabled_nfc), Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
        }

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // Create intent filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // Enable foreground dispatch for getting intent from NFC event
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[] {filter}, this.techList);
    }

    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void readInfo(IsoDep iso) {
        try {
            CommonCard c;
            String basicInfo = "";
            boolean flag = false;

            // 尝试读取金龙卡
            c = new JinLongCard(mVerboseInfo, this);
            if (c.selectAID(iso) != null) {
                flag = true;
                JinLongCard j = new JinLongCard(mVerboseInfo, this);
                Toast.makeText(this, "金龙卡", Toast.LENGTH_LONG).show();
                String cardType = j.getCardType(j.selectAID(iso));
                if (cardType.substring(0, 8).equals("SCUTCARD")) { // 尝试读取华工卡
                    j = new SCUTCard(mVerboseInfo, this);
                }
                basicInfo += j.getGeneralInfo(iso) + "======================";
                // return;
            }

            // 尝试读取广州大学城一卡通
            c = new HEMCCard(mVerboseInfo, this);
            if (c.selectAID(iso) != null) {
                flag = true;
                Toast.makeText(this, "大学城一卡通", Toast.LENGTH_LONG).show();
                //HEMCCard h = new HEMCCard(mVerboseInfo, this);
                basicInfo += c.getGeneralInfo(iso) + "======================";
                // return;
            }

            // 尝试读取羊城通
            c = new YangChengTong(mVerboseInfo, this);
            if (c.selectAID(iso) != null) {
                flag = true;
                Toast.makeText(this, "羊城通", Toast.LENGTH_LONG).show();
                basicInfo += c.getGeneralInfo(iso) + "======================";
            }

            if (flag)
                mBasicInfo.setText(Html.fromHtml(basicInfo));
            else // 不知道是什么卡
                mBasicInfo.setText(Html.fromHtml("<h1><font color=\"#ff0f0f\"><big>不知道是什么卡 TAT</big></font></h1>"));

        } catch (Exception ex) {
            commonCard.addVerbose(ex.toString(), "#ff0000");
            mBasicInfo.setText(Html.fromHtml("<h1><font color=\"#ff0ff0\"><big>Oops...程序出错啦</big></font></h1>"));
        }
    }

    protected void onNewIntent(Intent intent) {
        //if (!intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) return;
        //Tag tag = intent.getParcelableExtra(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            hideTip();
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            commonCard.addVerbose(getString(R.string.detected_card) + commonCard.byte2Str(tag.getId()), "#a03430");

            IsoDep iso = IsoDep.get(tag);
            if (iso == null) {
                commonCard.addVerbose(getString(R.string.not_isodep), "#ff0000");
                mBasicInfo.setText(Html.fromHtml(
                        "<h1><font color=\"#ff0f0f\">" + getString(R.string.not_isodep) + "</big></font></h1>"));
                return;
            }

            iso.connect();
            readInfo(iso);
            //mTextMessage.append("\n" + getString(R.string.reading));

        } catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }


}
