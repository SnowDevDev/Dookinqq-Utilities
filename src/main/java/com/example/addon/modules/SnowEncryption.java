package com.example.addon.modules;

import com.github.luben.zstd.Zstd;
import com.example.addon.AddonTemplate;
import com.example.addon.utils.Base91;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class SnowEncryption extends Module {
    public SnowEncryption(){
        super(AddonTemplate.CATEGORY,"Snow Encryption", "encrypts your chat messages with a key");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> magicString = sgGeneral.add(new StringSetting.Builder()
            .name("magic-string")
            .defaultValue("daisydaisyz")
            .build()
    );

    private final Setting<Boolean> useWhitelist = sgGeneral.add(new BoolSetting.Builder()
            .name("use-whitelist")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> whitelist = sgGeneral.add(new StringListSetting.Builder()
            .name("receive-whitelist-players")
            .visible(useWhitelist::get)
            .build()
    );

    private final Setting<Boolean> useBlacklist = sgGeneral.add(new BoolSetting.Builder()
            .name("use-blacklist")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> blacklist = sgGeneral.add(new StringListSetting.Builder()
            .name("receive-blacklist-players")
            .visible(useBlacklist::get)
            .build()
    );


    private final Setting<Encoding> encoder = sgGeneral.add(new EnumSetting.Builder<Encoding>()
            .name("encoding")
            .defaultValue(Encoding.Base64)
            .build()
    );

    private final Setting<Boolean> compress = sgGeneral.add(new BoolSetting.Builder()
            .name("compress")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> encryptionKey = sgGeneral.add(new StringSetting.Builder()
            .name("encryption-key")
            .defaultValue("poggot")
            .build());

    private final Setting<Boolean> encryptAll = sgGeneral.add(new BoolSetting.Builder()
            .name("encrypt-all")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> sendPrefix = sgGeneral.add(new StringSetting.Builder()
            .name("send-prefix")
            .defaultValue("!")
            .visible(() -> !encryptAll.get())
            .build());

    @EventHandler
    public void onSendMessageEvent(SendMessageEvent event) {
        if (sendPrefix.get().isBlank()) sendPrefix.reset();
        if (encryptAll.get() || (event.message.startsWith(sendPrefix.get()) && !encryptAll.get())) {
            String plainText = encryptAll.get() ? event.message : event.message.substring(sendPrefix.get().length());
            info(Text.of(Formatting.GREEN + "Sending Encrypted Message: " + plainText));
            String encrypt;
            try {
                encrypt = encrypt(plainText, encryptionKey.get());
            } catch (Exception e) {
                encrypt = "Failed to encrypt.";
            }
            event.message = magicString.get() + encrypt;
        }
    }

    @EventHandler
    public void onReceiveMessageEvent(ReceiveMessageEvent event) {
        if (magicString.get().isBlank()) magicString.reset();

        String message = event.getMessage().getString();
        int endOfUsernameIndex = message.indexOf('>');
        if (message.startsWith("<") && endOfUsernameIndex != -1) {
            String content = message.substring(endOfUsernameIndex + 1).trim();
            String username = message.substring(1, endOfUsernameIndex).trim();
            if (username.equals(mc.player.getName().getString())) return;
            if (useBlacklist.get() && blacklist.get().contains(username)) return;
            if (useWhitelist.get() && !whitelist.get().contains(username)) return;
            if (content.startsWith(magicString.get())) {
                String cipherText = content.substring(magicString.get().length());
                String decrypt;
                try {
                    decrypt = decrypt(cipherText, encryptionKey.get());
                } catch (Exception e) {
                    decrypt = "Failed to decrypt message.";
                }
                info(Formatting.GREEN + "Decrypted Message from " + username + ": " + decrypt);
            }
        }
    }


    private SecretKeySpec deriveKey(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
    }

    private String encrypt(String plainText, String password) throws Exception {
        SecretKeySpec key = deriveKey(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(compress.get() ? Zstd.compress(plainText.getBytes(StandardCharsets.UTF_8)) : plainText.getBytes(StandardCharsets.UTF_8));
        if (encoder.get().equals(Encoding.Base64)) {
            return Base64.getEncoder().withoutPadding().encodeToString(encrypted);
        } else {
            return Base91.encodeToString(encrypted);
        }
    }

    private String decrypt(String encodedCipherText, String password) throws Exception {
        SecretKeySpec key = deriveKey(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decoded;
        if (encoder.get().equals(Encoding.Base64)) {
            decoded = Base64.getDecoder().decode(encodedCipherText);
        } else {
            decoded = Base91.decode(encodedCipherText);
        }
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(compress.get() ? decompressZstd(decrypted) : decrypted, StandardCharsets.UTF_8);
    }

    private byte[] decompressZstd(byte[] compressedData) throws Exception {
        long originalSize = Zstd.decompressedSize(compressedData);
        byte[] decompressed = new byte[(int) originalSize];
        long actualSize = Zstd.decompress(decompressed, compressedData);
        if (actualSize != originalSize) throw new RuntimeException("Decompressed size mismatch");
        return decompressed;
    }

    private enum Encoding {
        Base64,
        Base91
    }
}
