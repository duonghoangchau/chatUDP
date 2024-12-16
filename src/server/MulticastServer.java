/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

/**
 *
 * @author Dell
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MulticastServer {

    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int PORT = 4446;
    private static final String SECRET_KEY_STRING = "MySuperSecretKey123"; // Khóa AES phải là 16 ký tự
    private SecretKey secretKey;
    private MulticastSocket socket;

    public MulticastServer() throws Exception {
        byte[] decodedKey = SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8);
        secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        socket = new MulticastSocket(PORT);
        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        socket.joinGroup(group);
    }

    public void start() {
        System.out.println("Server is running...");

        while (true) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Nhận tin nhắn từ client

                String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("Received: " + receivedMessage);

                // Giải mã tin nhắn
                String decryptedMessage = decrypt(receivedMessage, secretKey);
                System.out.println("Decrypted: " + decryptedMessage);

                // Nếu tin nhắn là file
                if (decryptedMessage.startsWith("FILE:")) {
                    handleFile(decryptedMessage);
                } else {
                    // Phát tán tin nhắn cho các client khác
                    broadcastMessage(receivedMessage, packet.getAddress(), packet.getPort());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFile(String decryptedMessage) throws IOException {
        String[] parts = decryptedMessage.split(":");
        if (parts.length >= 3) {
            String fileName = parts[1];
            byte[] fileData = Base64.getDecoder().decode(parts[2]);

            // Xử lý file nhận được
            System.out.println("Received file: " + fileName);
            // Có thể lưu file xuống máy chủ hoặc gửi tiếp cho các client khác
        }
    }

    private void broadcastMessage(String message, InetAddress senderAddress, int senderPort) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String decrypt(String encryptedMessage, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage.trim());
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        try {
            MulticastServer server = new MulticastServer();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
