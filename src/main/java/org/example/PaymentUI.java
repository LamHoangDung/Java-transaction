package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.request.PaymentRequest;
import org.example.service.PaymentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PaymentUI extends JFrame {

    // Các ô nhập thông tin.
    private final JTextField transactionIdField =
            new JTextField(UUID.randomUUID().toString());

    private final JTextField transactionNameField =
            new JTextField("Thanh toán hóa đơn thử nghiệm");

    private final JTextField merchantIdField =
            new JTextField("MERCHANT_X");

    // Các nút và dòng thông báo.
    private final JButton payButton = new JButton("Thanh toán");
    private final JButton newButton = new JButton("Giao dịch mới");

    private final JLabel statusLabel =
            new JLabel("Nhập thông tin và bấm Thanh toán.");

    private final PaymentService paymentService = new PaymentService();

    private boolean databaseReady = false;
    private boolean processing = false;

    public PaymentUI() {

        setTitle("Thanh toán giả lập neaPay");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel("Mã giao dịch"));
        panel.add(transactionIdField);
        panel.add(Box.createVerticalStrut(12));

        panel.add(new JLabel("Tên giao dịch"));
        panel.add(transactionNameField);
        panel.add(Box.createVerticalStrut(12));

        panel.add(new JLabel("Merchant"));
        panel.add(merchantIdField);
        panel.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(payButton);
        buttons.add(newButton);

        panel.add(buttons);
        panel.add(Box.createVerticalStrut(12));
        panel.add(statusLabel);

        // Giữ chiều cao các ô nhập.
        for (JTextField field : new JTextField[]{
                transactionIdField,
                transactionNameField,
                merchantIdField
        }) {
            field.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 30)
            );
        }

        add(panel);
        setSize(600, 350);
        setLocationRelativeTo(null);


        // Bấm nút thì gọi hàm xử lý.
        payButton.addActionListener(event -> submitPayment());
        newButton.addActionListener(event -> resetForm());

        // Chỉ đóng database khi ứng dụng kết thúc.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (processing) {
                    JOptionPane.showMessageDialog(
                            PaymentUI.this,
                            "Giao dịch đang xử lý. Vui lòng chờ."
                    );
                    return;
                }

                DatabaseConfig.close();
                dispose();
            }
        });
    }

    private void submitPayment() {
        PaymentRequest payment = new PaymentRequest();
        payment.setTransactionId(transactionIdField.getText().trim());
        payment.setTransactionName(transactionNameField.getText().trim());
        payment.setMerchantId(merchantIdField.getText().trim());

        if (payment.getTransactionId().isBlank()||payment.getTransactionName().isBlank()||payment.getMerchantId().isBlank()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        setProcessing(true);
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setText("Đang xử lý...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (!databaseReady) {
                    DatabaseConfig.init(new AppConfig());
                    databaseReady = true;
                }

                paymentService.Pay(payment);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Kiểm tra công việc nền có lỗi không.

                    statusLabel.setForeground(new Color(0, 128, 0));
                    statusLabel.setText("Thanh toán thành công");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(
                            "Có lỗi xảy ra, vui lòng kiểm tra lại chương trình."
                    );

                } catch (ExecutionException e) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(
                            "Có lỗi xảy ra, vui lòng kiểm tra lại chương trình."
                    );
                    e.getCause().printStackTrace();

                } finally {
                    setProcessing(false);
                }
            }
        };

        worker.execute();
    }
    private void setProcessing(boolean value) {
        processing = value;

        payButton.setEnabled(!value);
        newButton.setEnabled(!value);

        transactionIdField.setEnabled(!value);
        transactionNameField.setEnabled(!value);
        merchantIdField.setEnabled(!value);
    }

    private void resetForm() {
        transactionIdField.setText(UUID.randomUUID().toString());

        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText("Nhập thông tin và bấm Thanh toán.");

        payButton.setEnabled(true);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PaymentUI frame = new PaymentUI();
            frame.setVisible(true);
        });
    }
}