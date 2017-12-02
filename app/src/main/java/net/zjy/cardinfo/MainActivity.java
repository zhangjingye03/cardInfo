package net.zjy.cardinfo;

import android.app.PendingIntent;
import android.content.Context;
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

import net.zjy.cardinfo.ScutCard;
import net.zjy.cardinfo.CommonCard;

import java.nio.charset.Charset;
import java.util.Arrays;


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
        String cardType = "";
        try {
            cardType = commonCard.getCardType(iso);


            // CommonCard c;
            if (cardType.equals("SCUTCARD00000001")) {
                 ScutCard c = new ScutCard(mVerboseInfo, this, commonCard);
                 mBasicInfo.setText(Html.fromHtml(c.getGeneralInfo(iso)));
            } else if (cardType.equals("SYSUCARD00000001")) {
                SysuCard c = new SysuCard(mVerboseInfo, this, commonCard);
                mBasicInfo.setText(Html.fromHtml(c.getGeneralInfo(iso)));
            } else {
                commonCard.addVerbose(getString(R.string.unknown_card_type), "#913e1c");
                mBasicInfo.setText(Html.fromHtml("<h1><font color=\"#ff0ff0\"><big>Nothing to read...</big></font></h1>"));
            }

        } catch (Exception ex) {
            commonCard.addVerbose(ex.toString(), "#ff0000");
            mBasicInfo.setText(Html.fromHtml("<h1><font color=\"#ff0ff0\"><big>Oops...</big></font></h1>"));
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
                return;
            }

            iso.connect();
            readInfo(iso);
            //mTextMessage.append("\n" + getString(R.string.reading));

        } catch (Exception ex) {
            Toast.makeText(this, ex.getStackTrace().toString(), Toast.LENGTH_LONG).show();
        }
    }


}
