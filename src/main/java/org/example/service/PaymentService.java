package org.example.service;

import org.example.NeaPayMain;
import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.repository.TransactionRepository;
import org.example.request.PaymentRequest;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PaymentService {

    private final TransactionRepository repository = new TransactionRepository();

    public void Pay(PaymentRequest paymentRequest) {
        try (InputStream config = NeaPayMain.class.getResourceAsStream("/neapay-payment.xml");
             Socket socket = new Socket()) {

            GenericPackager packager = new GenericPackager(config);

            // giả lập các trường thông tin
            String stan = "900005";
            ISOMsg request = new ISOMsg();
            request.setPackager(packager);
            request.setMTI("0200");
            request.set(2, "9876500000306084"); // Thẻ mẫu
            request.set(3, "000000");          // Mã xử lý
            request.set(4, "000000010000");    //so tien thanh toan -> mã F39 từ server trả về sẽ lấy 2 số đầu của F4 theo như cấu hình giả lập của neapay
            request.set(11, stan);            // Theo dõi yêu cầu
            request.set(42, paymentRequest.getMerchantId());
            byte[] requestBytes = request.pack();

            //ket noi va gui yeu cai len server
            socket.connect(new InetSocketAddress("127.0.0.1", 9009), 5000);
            socket.setSoTimeout(20000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());


            output.writeShort(requestBytes.length); //2 byte do dai -> thong bao cho server biet ndung dai bao nhiey byte
            output.write(requestBytes); //ndung chinh : MTI, bitmap, fields
            output.flush(); //day dlieu len

            System.out.println("da gui yeu cau len server");


            DataInputStream input = new DataInputStream(socket.getInputStream());

            int length = input.readUnsignedShort(); //doc 2 byte dau cua phan hoi de biet ndung dai bao nhieu
            byte[] responseBytes = new byte[length]; //tao mang de chua đủ noi dung phản hồi
            input.readFully(responseBytes); //dua ndung vao mảng
            ISOMsg response = new ISOMsg();
            response.setPackager(packager);
            response.unpack(responseBytes);

            //xac nhan response tu server tra ve phai co cac field
            if (!"0210".equals(response.getMTI()) || !stan.equals(response.getString(11)) || !response.hasField(39)) {
                System.out.println("phan hoi khong dung yeu cau");
                return;
            }

            String responseCode = response.getString(39);
            System.out.println("Mã kết quả: " + responseCode);

            // chi insert DB khi server tra yeu cau chap nhan
            if ("00".equals(responseCode)) {
                int result = repository.insertSuccess(paymentRequest);
                if (result == 1) {
                    System.out.println("da luu thanh cong giao dich xuong DB");
                } else {
                    System.out.println("neapay tra yeu cau chap nhan nhung khong luu dc vo DB Gdich: " + paymentRequest.getTransactionId());
                }
            } else {
                System.out.println("Server khong chap nhan thanh toan");
            }
        } catch (Exception e) {
            System.err.println("Loi: " + e.getMessage());
        }
    }

}