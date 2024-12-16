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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;

public class UDPServer {

    private DatagramSocket socket;
    private boolean running;
    private static final String SECRET_KEY_STRING = "MySuperSecretKey";
    private SecretKey secretKey;
    private List<ClientInfo> clients;

    public UDPServer(int port) throws Exception {
        socket = new DatagramSocket(port);
        byte[] decodedKey = SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8);
        secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        clients = new ArrayList<>();
    }

    public void start() {
        running = true;
        System.out.println("Server is running...");

        while (running) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Nhận tin nhắn từ client

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                System.out.println("Received: " + receivedMessage);

                // Kiểm tra nếu client mới, thêm vào danh sách
                addClientIfNew(clientAddress, clientPort);

                // Xử lý tin nhắn từ client
                if (receivedMessage.equals("/quit")) {
                    removeClient(clientAddress, clientPort);
                    continue;
                }

                // Gửi tin nhắn đến tất cả các client khác
                String decryptedMessage = decrypt(receivedMessage, secretKey);
                System.out.println("Decrypted: " + decryptedMessage);
                String nickname = "Client " + clientPort;
                String formattedMessage = nickname + ": " + decryptedMessage;

                broadcastMessage(formattedMessage, clientAddress, clientPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addClientIfNew(InetAddress clientAddress, int clientPort) {
        boolean exists = clients.stream().anyMatch(client -> client.getAddress().equals(clientAddress) && client.getPort() == clientPort);
        if (!exists) {
            clients.add(new ClientInfo(clientAddress, clientPort));
            System.out.println("Added new client: " + clientAddress + ":" + clientPort);
        }
    }

    private void removeClient(InetAddress clientAddress, int clientPort) {
        clients.removeIf(client -> client.getAddress().equals(clientAddress) && client.getPort() == clientPort);
        System.out.println("Removed client: " + clientAddress + ":" + clientPort);
    }

    private void broadcastMessage(String message, InetAddress senderAddress, int senderPort) {
        try {
            for (ClientInfo client : clients) {
                if (client.getAddress().equals(senderAddress) && client.getPort() == senderPort) {
                    continue; // Không gửi lại tin nhắn cho chính client đã gửi
                }

                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.getAddress(), client.getPort());
                socket.send(packet); // Gửi tin nhắn tới tất cả các client
            }
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
            UDPServer server = new UDPServer(12345);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Class để lưu thông tin client
    private static class ClientInfo {
        private InetAddress address;
        private int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }
    }
}