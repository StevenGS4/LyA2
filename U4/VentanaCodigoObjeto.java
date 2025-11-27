import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;

public class VentanaCodigoObjeto extends JDialog {

    private JTextArea textArea;

    public VentanaCodigoObjeto(Frame parent, String codigoASM) {
        super(parent, "Código Objeto Generado (ASM)", true);
        setSize(750, 600);
        setLocationRelativeTo(parent);

        textArea = new JTextArea();
        textArea.setText(codigoASM);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(textArea);

        JButton btnGuardar = new JButton("Guardar como archivo .ASM");
        btnGuardar.addActionListener(e -> guardarArchivo());

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);

        add(scroll, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void guardarArchivo() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar archivo ensamblador");

        if (selector.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(selector.getSelectedFile() + ".asm")) {
                fw.write(textArea.getText());
                JOptionPane.showMessageDialog(this, "Archivo guardado correctamente.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivo: " + ex.getMessage());
            }
        }
    }
}
