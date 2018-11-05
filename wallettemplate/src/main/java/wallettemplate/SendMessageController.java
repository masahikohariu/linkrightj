/*
 * Copyright by the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wallettemplate;

import javafx.scene.layout.HBox;
import javafx.scene.control.TextArea;

import org.bitcoinj.core.*;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;

import org.spongycastle.crypto.params.KeyParameter;
import wallettemplate.controls.BitcoinAddressValidator;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.WTUtils;

import static com.google.common.base.Preconditions.checkState;
import static wallettemplate.Main.bitcoin;
import static wallettemplate.utils.GuiUtils.*;

import javax.annotation.Nullable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

public class SendMessageController {
    public Button sendBtn;
    public Button cancelBtn;
    public TextField address;
    public ComboBox addressList;
    public Label titleLabel;
    public TextField amountEdit;
    public Label btcLabel;
    public TextArea message;

    public Main.OverlayUI overlayUI;

    private Wallet.SendResult sendResult;
    private KeyParameter aesKey;
    private DB db;
    
    public static final String APP_SENT_ADDRESS_LIST = "SentAddressList";
    // Called by FXMLLoader
    public void initialize() {
        Coin balance = Main.bitcoin.wallet().getBalance();
        
        checkState(!balance.isZero());
        new BitcoinAddressValidator(Main.params, address, sendBtn);
        new TextFieldValidator(amountEdit, text ->
                !WTUtils.didThrow(() -> checkState(Coin.parseCoin(text).compareTo(balance) <= 0)));
        System.out.println(balance.value);
        double amount = (balance.value - 400000)/100000000.0;
        if (amount>0) {
        	amountEdit.setText(String.valueOf(amount));
        }else {
        	amountEdit.setText(balance.toPlainString());
        }
        db = bitcoin.getDB();
        refreshAddressList();
        address.setText(Main.bitcoin.wallet().currentAddress(KeyPurpose.RECEIVE_FUNDS).toString());
        if (addressList.getItems().size()>0) {
        	address.setText(addressList.getValue().toString());
        }
        addressList.valueProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue ov, String t, String t1) {     
            	if (t1 !=null) {
            		address.setText( t1);
            	}
            }    
        });
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }
    public void refreshAddressList() {
    	BTreeMap btmap = db.treeMap(APP_SENT_ADDRESS_LIST).createOrOpen();
    	addressList.getItems().clear();
    	System.out.println("refreshaddresslist");
    	long maxTimes=0;
    	String preferAddress;
    	int preferIndex=0;
    	if (btmap.getSize()>0) {
    		addressList.getItems().addAll(btmap.getKeys().toArray());
    		int i=0;
    		for (Object addr : addressList.getItems()) {
    			//System.out.println("addresslist:" +addr.toString() +", "+ btmap.get(addr).toString());
    			if (Long.parseLong(btmap.get(addr).toString())>maxTimes) {
    				maxTimes=Long.parseLong(btmap.get(addr).toString());
    				preferIndex=i;
    			}
    			i=+1;
    			
    		}
    		addressList.setValue(addressList.getItems().get(preferIndex));
    		
    	}
    }
    
    public void send(ActionEvent event) {
        // Address exception cannot happen as we validated it beforehand.
        try {
            Coin amount = Coin.parseCoin(amountEdit.getText());
            LegacyAddress destination = LegacyAddress.fromBase58(Main.params, address.getText());
            SendRequest req;
        	String msg = message.getText();
        	if (msg.length()>2826) {
        		Alert alert = new Alert(AlertType.ERROR);            		
        		alert.setTitle("Send Message");
        		String str = "Message should be less than 2826 characters";
        		alert.setContentText(str);
        		alert.showAndWait();
        		
        		return;
        	}            	     
        	
            if (amount.equals(Main.bitcoin.wallet().getBalance()))
                req = SendRequest.sendMessage(destination, amount,message.getText());
            else {
            	req = SendRequest.sendMessage(destination, amount,message.getText());
            }
            req.aesKey = aesKey;
            sendResult = Main.bitcoin.wallet().sendCoins(req);
			
            BTreeMap btmap = db.treeMap(APP_SENT_ADDRESS_LIST).createOrOpen();
            long nTimes=1;
			if (btmap.containsKey(address.getText())) {
				nTimes=Long.parseLong(btmap.get(address.getText()).toString());
				nTimes+=1;
			}
			btmap.put(address.getText(), nTimes); 
			refreshAddressList();
			
			
            Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    checkGuiThread();
                    overlayUI.done();
                }

                @Override
                public void onFailure(Throwable t) {
                    // We died trying to empty the wallet.
                    crashAlert(t);
                }
            });
            sendResult.tx.getConfidence().addEventListener((tx, reason) -> {
                if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS)
                    updateTitleForBroadcast();
            });            
            sendBtn.setDisable(true);
            address.setDisable(true);
            ((HBox)amountEdit.getParent()).getChildren().remove(amountEdit);
            ((HBox)btcLabel.getParent()).getChildren().remove(btcLabel);
            
            updateTitleForBroadcast();
			
            
        } catch (InsufficientMoneyException e) {
            informationalAlert("Could not empty the wallet",
                    "You may have too little money left in the wallet to make a transaction.");
            overlayUI.done();
        
        } catch (ECKey.KeyIsEncryptedException e) {
            askForPasswordAndRetry();
        }
    }

    private void askForPasswordAndRetry() {
        Main.OverlayUI<WalletPasswordController> pwd = Main.instance.overlayUI("wallet_password.fxml");
        final String addressStr = address.getText();
        final String amountStr = amountEdit.getText();
        pwd.controller.aesKeyProperty().addListener((observable, old, cur) -> {
            // We only get here if the user found the right password. If they don't or they cancel, we end up back on
            // the main UI screen. By now the send money screen is history so we must recreate it.
            checkGuiThread();
            Main.OverlayUI<SendMessageController> screen = Main.instance.overlayUI("send_money.fxml");
            screen.controller.aesKey = cur;
            screen.controller.address.setText(addressStr);
            screen.controller.amountEdit.setText(amountStr);
            screen.controller.send(null);
        });
    }

    private void updateTitleForBroadcast() {
        final int peers = sendResult.tx.getConfidence().numBroadcastPeers();
        titleLabel.setText(String.format("Broadcasting ... seen by %d peers", peers));
    }
}