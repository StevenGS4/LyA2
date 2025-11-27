
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class INTERFAZ extends javax.swing.JFrame {

    private File archivoActual = null;
    private UndoManager undoManager = new UndoManager();
    private int contadorClicks = 0;
    private File archivoCopiado = null;
    private File carpetaActual = null;
    private Timer resaltadoTimer;
    GeneradorCodigoIntermedio generador;
    GeneradorCódigoObjeto generador2;
    TextLineNumber lineNumberComponent;

    // DEFINICIÓN DE COLORES PARA CADA TIPO DE TOKEN
    private void colors() {
        final StyleContext cont = StyleContext.getDefaultStyleContext();

        final AttributeSet attPalabraClave = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(0, 0, 255)); // Azul para palabras clave

        final AttributeSet attComandoMovimiento = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(255, 140, 0)); // Naranja para movimientos

        final AttributeSet attComandoActuador = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(128, 0, 128)); // Púrpura para actuadores

        final AttributeSet attComandoSensor = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(0, 128, 128)); // Verde azulado para sensores

        final AttributeSet attComandoTiempo = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(255, 20, 147)); // Rosa para tiempo

        final AttributeSet attNumero = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(255, 0, 0)); // Rojo para números

        final AttributeSet attOperadorAritmetico = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(139, 69, 19)); // Marrón para operadores aritméticos

        final AttributeSet attOperadorRel = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(34, 139, 34)); // Verde para operadores relacionales

        final AttributeSet attOperadorLogico = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(220, 20, 60)); // Carmesí para operadores lógicos

        final AttributeSet attOperadorAsignacion = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(70, 130, 180)); // Azul acero para asignación

        final AttributeSet attUnidadTiempo = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(255, 165, 0)); // Dorado para unidades de tiempo

        final AttributeSet attTiposDatos = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(0, 100, 0)); // Verde oscuro para tipos de datos

        final AttributeSet attParentesis = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(105, 105, 105)); // Gris para paréntesis

        final AttributeSet attComentario = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(128, 128, 128)); // Gris para comentarios

        final AttributeSet attSeparador = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(75, 0, 130)); // Índigo para separadores

        final AttributeSet attDefault = cont.addAttribute(cont.getEmptySet(),
                StyleConstants.Foreground, new Color(0, 0, 0)); // Negro por defecto

        // DOCUMENTO CON RESALTADO AUTOMÁTICO
        DefaultStyledDocument doc = new DefaultStyledDocument() {
            @Override
            public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offset, str, a);
                SwingUtilities.invokeLater(() -> aplicarResaltado());
            }

            @Override
            public void remove(int offs, int len) throws BadLocationException {
                super.remove(offs, len);
                SwingUtilities.invokeLater(() -> aplicarResaltado());
            }

            private void aplicarResaltado() {
                try {
                    String texto = getText(0, getLength());

                    // Reset todo a negro
                    setCharacterAttributes(0, getLength(), attDefault, false);

                    // APLICAR CADA PATRÓN EN ORDEN ESPECÍFICO
                    // 1. COMENTARIOS (primero para que no sean sobrescritos)
                    aplicarPatron(texto, "(//.|#.)", attComentario);

                    // 2. PALABRAS CLAVE COMPUESTAS (antes que las simples)
                    aplicarPatron(texto, "\\b(fin si|fin para|fin mientras|fin caso)\\b", attPalabraClave);
                    aplicarPatron(texto, "\\b(iniciar_metodo|fin_metodo)\\b", attPalabraClave);

                    // 3. PALABRAS CLAVE SIMPLES
                    aplicarPatron(texto, "\\b(iniciar|Terminar|si|entonces|detener|para|mientras|caso|hacer|hasta|cuando|romper|fin|tipo)\\b", attPalabraClave);

                    // 4. COMANDOS ESPECÍFICOS
                    aplicarPatron(texto, "\\b(mover_adelante|mover_atras|girar_izquierda|girar_derecha)\\b", attComandoMovimiento);
                    aplicarPatron(texto, "\\b(encender_led|apagar_led)\\b", attComandoActuador);
                    aplicarPatron(texto, "\\b(leer_sensor)\\b", attComandoSensor);
                    aplicarPatron(texto, "\\b(esperar)\\b", attComandoTiempo);

                    // 5. TIPOS DE DATOS
                    aplicarPatron(texto, "\\b(numero|Sensor)\\b", attTiposDatos);

                    // 6. UNIDADES DE TIEMPO
                    aplicarPatron(texto, "\\b(segundos|milisegundos)\\b", attUnidadTiempo);

                    // 7. NÚMEROS
                    aplicarPatron(texto, "\\b\\d+(\\.\\d+)?\\b", attNumero);

                    // 8. OPERADORES (en orden de precedencia)
                    aplicarPatron(texto, "(<=|>=|==|!=)", attOperadorRel);
                    aplicarPatron(texto, "(<|>)", attOperadorRel);
                    aplicarPatron(texto, "(\\&\\&|\\|\\||!)", attOperadorLogico);
                    aplicarPatron(texto, "(\\+|\\-|\\*|\\/)", attOperadorAritmetico);
                    aplicarPatron(texto, "=", attOperadorAsignacion);

                    // 9. SÍMBOLOS
                    aplicarPatron(texto, "(\\(|\\))", attParentesis);
                    aplicarPatron(texto, ",", attSeparador);

                } catch (BadLocationException e) {
                    System.err.println("Error en resaltado: " + e.getMessage());
                }
            }

            private void aplicarPatron(String texto, String regex, AttributeSet estilo) {
                try {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(texto);

                    while (matcher.find()) {
                        int inicio = matcher.start();
                        int longitud = matcher.end() - inicio;
                        setCharacterAttributes(inicio, longitud, estilo, false);
                    }
                } catch (Exception e) {
                    System.err.println("Error aplicando patrón " + regex + ": " + e.getMessage());
                }
            }
        };

        // Aplicar el documento con resaltado al JTextPane
        String textoActual = JTAEditotText.getText();
        JTAEditotText.setStyledDocument(doc);
        JTAEditotText.setText(textoActual);
    }

    public INTERFAZ() {
        initComponents();
        configurarUndoRedo();
        colors();
        lineNumberComponent = new TextLineNumber(JTAEditotText);
        jScrollPane4.setRowHeaderView(lineNumberComponent);

        JTAEditotText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String texto = JTAEditotText.getText();

                    // Buscar la posición de "Terminar"
                    int posicionTerminar = texto.lastIndexOf("Terminar");

                    if (posicionTerminar != -1) {
                        // Calcular donde termina la palabra "Terminar"
                        int finTerminar = posicionTerminar + "Terminar".length();

                        // Si hay texto después de "Terminar", eliminarlo
                        if (texto.length() > finTerminar) {
                            JTAEditotText.setText(texto.substring(0, finTerminar));
                        }
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        JPEditorTexto = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        JTAEditotText = new javax.swing.JTextPane();
        JPCarpeta = new javax.swing.JPanel();
        btnCambiarCarpeta = new javax.swing.JButton();
        JPAnalisis = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        JPEditorTextAnalisis = new javax.swing.JEditorPane();
        JPConsola = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        JTAConsola = new javax.swing.JTextPane();
        JBMBarra = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        JCBNuevo = new javax.swing.JCheckBoxMenuItem();
        JCBAbrir = new javax.swing.JCheckBoxMenuItem();
        JCBGuardar = new javax.swing.JCheckBoxMenuItem();
        JCBAyuda = new javax.swing.JCheckBoxMenuItem();
        JMTablaSimbolos = new javax.swing.JMenu();
        JMCompilar = new javax.swing.JMenu();
        JMCompilacion = new javax.swing.JMenu();
        JMEjecutar = new javax.swing.JMenu();
        JMAnalisisLexico = new javax.swing.JMenu();
        JMSintactico = new javax.swing.JMenu();
        JMAnalsisXSemantico = new javax.swing.JMenu();
        JMAnalisisSemantico = new javax.swing.JMenu();
        JMOptimizador = new javax.swing.JMenu();
        JMTraducirPrograma = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("COMPILADOR");

        JPEditorTexto.setBackground(new java.awt.Color(255, 255, 255));

        JTAEditotText.setBorder(javax.swing.BorderFactory.createTitledBorder("Editor de Texto"));
        JTAEditotText.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        JTAEditotText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                JTAEditotTextKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(JTAEditotText);

        javax.swing.GroupLayout JPEditorTextoLayout = new javax.swing.GroupLayout(JPEditorTexto);
        JPEditorTexto.setLayout(JPEditorTextoLayout);
        JPEditorTextoLayout.setHorizontalGroup(
            JPEditorTextoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPEditorTextoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4)
                .addContainerGap())
        );
        JPEditorTextoLayout.setVerticalGroup(
            JPEditorTextoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPEditorTextoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addContainerGap())
        );

        JPCarpeta.setBackground(new java.awt.Color(255, 255, 255));
        JPCarpeta.setBorder(javax.swing.BorderFactory.createTitledBorder("PROGRAMAS"));
        JPCarpeta.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JPCarpetaMouseClicked(evt);
            }
        });

        btnCambiarCarpeta.setText("Cambiar Carpeta");
        btnCambiarCarpeta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCambiarCarpetaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout JPCarpetaLayout = new javax.swing.GroupLayout(JPCarpeta);
        JPCarpeta.setLayout(JPCarpetaLayout);
        JPCarpetaLayout.setHorizontalGroup(
            JPCarpetaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPCarpetaLayout.createSequentialGroup()
                .addComponent(btnCambiarCarpeta)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        JPCarpetaLayout.setVerticalGroup(
            JPCarpetaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPCarpetaLayout.createSequentialGroup()
                .addComponent(btnCambiarCarpeta)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        JPAnalisis.setBackground(new java.awt.Color(255, 255, 255));
        JPAnalisis.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JPAnalisisMouseClicked(evt);
            }
        });

        JPEditorTextAnalisis.setEditable(false);
        JPEditorTextAnalisis.setBorder(javax.swing.BorderFactory.createTitledBorder("ANALISIS"));
        JPEditorTextAnalisis.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        JPEditorTextAnalisis.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JPEditorTextAnalisisMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(JPEditorTextAnalisis);
        JPEditorTextAnalisis.getAccessibleContext().setAccessibleName("ANÁLISIS");

        javax.swing.GroupLayout JPAnalisisLayout = new javax.swing.GroupLayout(JPAnalisis);
        JPAnalisis.setLayout(JPAnalisisLayout);
        JPAnalisisLayout.setHorizontalGroup(
            JPAnalisisLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPAnalisisLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
        );
        JPAnalisisLayout.setVerticalGroup(
            JPAnalisisLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPAnalisisLayout.createSequentialGroup()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );

        JPConsola.setBackground(new java.awt.Color(255, 255, 255));

        JTAConsola.setBorder(javax.swing.BorderFactory.createTitledBorder("CONSOLA"));
        JTAConsola.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        JTAConsola.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JTAConsolaMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(JTAConsola);

        javax.swing.GroupLayout JPConsolaLayout = new javax.swing.GroupLayout(JPConsola);
        JPConsola.setLayout(JPConsolaLayout);
        JPConsolaLayout.setHorizontalGroup(
            JPConsolaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPConsolaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 913, Short.MAX_VALUE)
                .addContainerGap())
        );
        JPConsolaLayout.setVerticalGroup(
            JPConsolaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JPConsolaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addContainerGap())
        );

        jMenu1.setText("Archivo");

        JCBNuevo.setText("Nuevo");
        JCBNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JCBNuevoActionPerformed(evt);
            }
        });
        jMenu1.add(JCBNuevo);

        JCBAbrir.setText("Abrir");
        JCBAbrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JCBAbrirActionPerformed(evt);
            }
        });
        jMenu1.add(JCBAbrir);

        JCBGuardar.setText("Guardar");
        JCBGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JCBGuardarActionPerformed(evt);
            }
        });
        jMenu1.add(JCBGuardar);

        JCBAyuda.setText("Ayuda");
        JCBAyuda.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JCBAyudaMouseClicked(evt);
            }
        });
        JCBAyuda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JCBAyudaActionPerformed(evt);
            }
        });
        jMenu1.add(JCBAyuda);

        JBMBarra.add(jMenu1);

        JMTablaSimbolos.setText("Tabla de Símbolos");
        JMTablaSimbolos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMTablaSimbolosMouseClicked(evt);
            }
        });
        JBMBarra.add(JMTablaSimbolos);

        JMCompilar.setText("ArbolSintatctico");
        JMCompilar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMCompilarMouseClicked(evt);
            }
        });
        JBMBarra.add(JMCompilar);

        JMCompilacion.setText("Compilar");
        JMCompilacion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMCompilacionMouseClicked(evt);
            }
        });
        JBMBarra.add(JMCompilacion);

        JMEjecutar.setText("Ejecutar");
        JMEjecutar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMEjecutarMouseClicked(evt);
            }
        });
        JBMBarra.add(JMEjecutar);

        JMAnalisisLexico.setText("Análisis Léxico");
        JMAnalisisLexico.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMAnalisisLexicoMouseClicked(evt);
            }
        });
        JBMBarra.add(JMAnalisisLexico);

        JMSintactico.setText("Análisis Sintáctico");
        JMSintactico.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMSintacticoMouseClicked(evt);
            }
        });
        JBMBarra.add(JMSintactico);

        JMAnalsisXSemantico.setText("Analisis Semantico");
        JMAnalsisXSemantico.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMAnalsisXSemanticoMouseClicked(evt);
            }
        });
        JBMBarra.add(JMAnalsisXSemantico);

        JMAnalisisSemantico.setText("Código Intermedio");
        JMAnalisisSemantico.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMAnalisisSemanticoMouseClicked(evt);
            }
        });
        JBMBarra.add(JMAnalisisSemantico);

        JMOptimizador.setText("Optimizador");
        JMOptimizador.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMOptimizadorMouseClicked(evt);
            }
        });
        JBMBarra.add(JMOptimizador);

        JMTraducirPrograma.setText("Código Objeto");
        JMTraducirPrograma.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JMTraducirProgramaMouseClicked(evt);
            }
        });
        JBMBarra.add(JMTraducirPrograma);

        setJMenuBar(JBMBarra);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(JPCarpeta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JPAnalisis, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(JPConsola, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JPEditorTexto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(JPEditorTexto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JPCarpeta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(JPAnalisis, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JPConsola, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void JCBGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JCBGuardarActionPerformed
        if (archivoActual == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar archivo como...");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto", "txt"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                archivoActual = fileChooser.getSelectedFile();
                if (!archivoActual.getName().toLowerCase().endsWith(".txt")) {
                    archivoActual = new File(archivoActual.getAbsolutePath() + ".txt");
                }
                cargarArchivosEnPanel(archivoActual.getParentFile());
            } else {
                return;
            }
        }
        try {
            Files.write(archivoActual.toPath(), JTAEditotText.getText().getBytes());
            JTAConsola.setText("Archivo guardado: " + archivoActual.getAbsolutePath());
            cargarArchivosEnPanel(archivoActual.getParentFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo");
        }
    }//GEN-LAST:event_JCBGuardarActionPerformed

    private void JCBAyudaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JCBAyudaActionPerformed
        mostrarManual();
    }//GEN-LAST:event_JCBAyudaActionPerformed


    private void JCBNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JCBNuevoActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona la carpeta para crear el nuevo archivo");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File carpeta = fileChooser.getSelectedFile();
            String nombreArchivo = JOptionPane.showInputDialog(this, "Nombre del archivo (sin .txt):");
            if (nombreArchivo != null && !nombreArchivo.trim().isEmpty()) {
                File nuevoArchivo = new File(carpeta, nombreArchivo + ".txt");
                try {
                    if (nuevoArchivo.createNewFile()) {
                        JTAEditotText.setText("");
                        archivoActual = nuevoArchivo;
                        cargarArchivosEnPanel(carpeta);
                        JTAConsola.setText("Archivo creado: " + nuevoArchivo.getAbsolutePath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al crear el archivo");
                }
            }
        }
    }//GEN-LAST:event_JCBNuevoActionPerformed

    private void JMcodigoIntermedioMouseClicked(java.awt.event.MouseEvent evt) {
        String codigo = JTAEditotText.getText();
        StringBuilder errores = new StringBuilder();

        // 1. Análisis léxico
        List<Analizador.Token> tokens = Analizador.analizarLexico(codigo);

        // 2. Obtener tabla de símbolos
        List<Analizador.EntradaTablaSimbolos> tablaSimbolos = Analizador.getTablaSimbolosCompleta();

        // 3. Análisis sintáctico y generación de AST
        AnalizadorSintactico analizador = new AnalizadorSintactico(tokens, tablaSimbolos);
        ProgramaNodo programaAST = analizador.parsear(tokens, tablaSimbolos);

        // Verificar errores
        if (analizador.getErrores().length() > 0) {
            return;
        }

        if (programaAST == null) {
            JTAConsola.setText("Error: No se pudo construir el AST");
            return;
        }

        // 4. Generar y mostrar cuádruplos
        GeneradorCodigoIntermedio generador = analizador.getGeneradorCodigo();
        if (generador == null) {
            JTAConsola.setText("Error: Generador de código no inicializado");
            return;
        }

        String cuadruplos = generador.obtenerCuadruplosEnFormatoTabla();
        JPEditorTextAnalisis.setText("Árbol Sintáctico:\n" + programaAST.mostrarArbol()
                + "\n\nCuádruplos generados:\n" + cuadruplos);
        JTAConsola.setText("Compilación exitosa. Cuádruplos generados.");

    }

    private void JCBAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JCBAbrirActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir archivo");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto", "txt"));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            archivoActual = fileChooser.getSelectedFile();
            try {
                String contenido = new String(Files.readAllBytes(archivoActual.toPath()), StandardCharsets.UTF_8);
                JTAEditotText.setText(contenido);
                cargarArchivosEnPanel(archivoActual.getParentFile());
                JTAConsola.setText("Archivo cargado: " + archivoActual.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo");
            }
        }
    }//GEN-LAST:event_JCBAbrirActionPerformed

    private void JPEditorTextAnalisisMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JPEditorTextAnalisisMouseClicked
        mostrarContenidoAnalisis();
    }//GEN-LAST:event_JPEditorTextAnalisisMouseClicked

    private void JPAnalisisMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JPAnalisisMouseClicked
        mostrarContenidoAnalisis();
    }//GEN-LAST:event_JPAnalisisMouseClicked

    private void JPCarpetaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JPCarpetaMouseClicked
        if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {
            mostrarMenuPegarEnPanel(evt);
        }
    }//GEN-LAST:event_JPCarpetaMouseClicked

    private void btnCambiarCarpetaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCambiarCarpetaActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona una carpeta");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File carpetaSeleccionada = fileChooser.getSelectedFile();
            cargarArchivosEnPanel(carpetaSeleccionada);
            archivoActual = null;
            JTAEditotText.setText("");
        }
    }//GEN-LAST:event_btnCambiarCarpetaActionPerformed

    private void JTAConsolaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JTAConsolaMouseClicked
        contadorClicks++;
        if (contadorClicks == 2) {
            mostrarContenidoConsola();
            contadorClicks = 0;
        }
        Timer timer = new Timer(1000, e -> contadorClicks = 0);
        timer.setRepeats(false);
        timer.start();
    }//GEN-LAST:event_JTAConsolaMouseClicked

    private void JTAEditotTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_JTAEditotTextKeyReleased
        int codigo = evt.getKeyCode();
        if (codigo == KeyEvent.VK_SPACE || codigo == KeyEvent.VK_ENTER) {
        }
    }//GEN-LAST:event_JTAEditotTextKeyReleased

    private void JMAnalisisLexicoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMAnalisisLexicoMouseClicked
        String codigo = JTAEditotText.getText();
        List<Analizador.Token> tokens = Analizador.analizarLexico(codigo);
        StringBuilder resultado = new StringBuilder();
        int lineaActual = -1;
        for (Analizador.Token token : tokens) {
            if (token.linea != lineaActual) {
                lineaActual = token.linea;
                resultado.append("\n[Línea ").append(lineaActual).append("]\n");
            }
            resultado.append(token.tipo)
                    .append(": ")
                    .append(token.lexema)
                    .append(" (col ").append(token.columna).append(")\n");
        }
        JPEditorTextAnalisis.setText(resultado.toString());

    }//GEN-LAST:event_JMAnalisisLexicoMouseClicked

    private void JMSintacticoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMSintacticoMouseClicked
        String codigo = JTAEditotText.getText();
        List<Analizador.Token> tokens = Analizador.analizarLexico(codigo);
        StringBuilder errores = new StringBuilder();
        boolean correcto = Analizador.analizarSintaxis(tokens, errores);
        if (correcto) {
            StringBuilder resultado = new StringBuilder();
            resultado.append("✅ Análisis sintáctico correcto.\n");
            resultado.append("Expresiones reconocidas por línea:\n");
            resultado.append(Analizador.obtenerPatronesPorLinea(tokens));
            JPEditorTextAnalisis.setText(resultado.toString());

            String simbolos = Analizador.getTablaSimbolosTexto();

            //mostrar la tabla de símbolos
            StringBuilder contenido = new StringBuilder();
            contenido.append("Tabla de símbolos generada:");

            if (simbolos.isEmpty()) {
                contenido.append("No se encontraron símbolos");
            }

            JTAConsola.setText(contenido.toString());
            // System.out.println(Analizador.analizarSintaxis(tokens, errores));

        } else {
            JPEditorTextAnalisis.setText("❌ Se encontraron errores en la sintaxis:\n" + errores.toString());
        }
    }//GEN-LAST:event_JMSintacticoMouseClicked

    private void JCBAyudaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JCBAyudaMouseClicked
        mostrarManual();
    }//GEN-LAST:event_JCBAyudaMouseClicked

    private void JMCompilarMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMCompilarMouseClicked
        String codigo = JTAEditotText.getText();
        Analizador compilador = new Analizador();

        List<Analizador.Token> tokens = compilador.analizarLexico(codigo);
        //Añadir otra validacion para la tabla de símbolos

        StringBuilder errores = new StringBuilder();

        boolean esValido = compilador.analizarSintaxis(tokens, errores);

        if (esValido) {
            JTAConsola.setText("✅ Compilación exitosa. No se encontraron errores.\nGuardando archivo...");
            if (archivoActual != null) {
                try (FileWriter writer = new FileWriter(archivoActual)) {
                    writer.write(codigo);
                    JTAConsola.setText(JTAConsola.getText() + "\nArchivo guardado exitosamente en: " + archivoActual.getAbsolutePath());
                } catch (IOException e) {
                    JTAConsola.setText(JTAConsola.getText() + "\nError al guardar el archivo: " + e.getMessage());
                }
            } else {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar archivo compilado");
                int userSelection = fileChooser.showSaveDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    archivoActual = fileChooser.getSelectedFile();
                    try (FileWriter writer = new FileWriter(archivoActual)) {
                        writer.write(codigo);
                        JTAConsola.setText(JTAConsola.getText() + "\nArchivo guardado exitosamente en: " + archivoActual.getAbsolutePath());
                        cargarArchivosEnPanel(archivoActual.getParentFile());
                    } catch (IOException e) {
                        JTAConsola.setText(JTAConsola.getText() + "\nError al guardar el archivo: " + e.getMessage());
                    }
                } else {
                    JTAConsola.setText(JTAConsola.getText() + "\nCancelado por el usuario.");
                }
            }
        } else {
            String[] lineas = codigo.split("\n");
            String[] mensajes = errores.toString().split("\n");
            StringBuilder salida = new StringBuilder("❌ Se encontraron errores en la compilación:\n\n");
            for (String mensaje : mensajes) {
                Matcher matcher = Pattern.compile("en línea (\\d+)").matcher(mensaje);
                if (matcher.find()) {
                    int numLinea = Integer.parseInt(matcher.group(1));
                    String lineaCodigo = (numLinea <= lineas.length) ? lineas[numLinea - 1].trim() : "";
                    salida.append("L").append(numLinea).append(" | ").append(lineaCodigo).append("\n");
                    salida.append("    → ").append(mensaje).append("\n\n");
                } else {
                    salida.append(mensaje).append("\n");
                }
            }
            JTAConsola.setText(salida.toString());
        }
        StringBuilder resultado = new StringBuilder();
        int lineaActual = -1;
        resultado.append("Análisis léxico.\n");
        resultado.append("-------------------------------------------------------------\n");

        for (Analizador.Token token : tokens) {
            if (token.linea != lineaActual) {
                lineaActual = token.linea;

                resultado.append("\n[Línea ").append(lineaActual).append("]\n");
            }
            resultado.append(token.tipo)
                    .append(": ")
                    .append(token.lexema)
                    .append(" (col ").append(token.columna).append(")\n");
        }
        JPEditorTextAnalisis.setText(resultado.toString());
        if (esValido) {
            resultado.append("\n");
            resultado.append("\n");
            resultado.append("\n");
            resultado.append("-------------------------------------------------------------\n");
            resultado.append("\n");
            resultado.append("\n");
            resultado.append("✅ Análisis sintáctico correcto.\n");
            resultado.append("Expresiones reconocidas por línea:\n");
            resultado.append(Analizador.obtenerPatronesPorLinea(tokens));
            // JPEditorTextAnalisis.setText(resultado.toString());

            resultado.append("Tabla de símbolos resultannte\n");
            resultado.append("-------------------------------------------------------------\n");
            resultado.append(mostrarTablaSimbolos(codigo));

            //aquí muestra el árbol
            ProgramaNodo arbol = Analizador.parsearAST(codigo, errores);
            resultado.append("\n");
            resultado.append("Generación del árbol sintáctico.\n");
            resultado.append("-------------------------------------------------------------\n");

            resultado.append(arbol.mostrarArbol() + "\n");

            JPEditorTextAnalisis.setText(resultado.toString());

            System.out.println(arbol.mostrarArbol());
        } else {
            JPEditorTextAnalisis.setText("❌ Se encontraron errores en la sintaxis:\n" + errores.toString());
        }

        ///////////////////////////cuadruplosssssss
    List<Analizador.EntradaTablaSimbolos> tablaSimbolos = Analizador.getTablaSimbolosCompleta();

        AnalizadorSintactico analizador = new AnalizadorSintactico(tokens, tablaSimbolos);
        ProgramaNodo programaAST = analizador.parsear(tokens, tablaSimbolos);

        // Verificar errores
        if (analizador.getErrores().length() > 0) {
            return;
        }

        if (programaAST == null) {
            JTAConsola.setText("Error: No se pudo construir el AST");
            return;
        }

        generador = analizador.getGeneradorCodigo();
        if (generador == null) {
            JTAConsola.setText("Error: Generador de código no inicializado");
            return;
        }

        //String cuadruplos = generador.obtenerCuadruplosEnFormatoTabla();
        //JPEditorTextAnalisis.setText("Árbol Sintáctico:\n" + programaAST.mostrarArbol() + 
        //                     "\n\nCuádruplos generados:\n" + cuadruplos);
        //Compilacion Exitosa. Cuádruplos generados.
        JTAConsola.setText("Árbol Sintáctico exitosa.");

        JPEditorTextAnalisis.setText("Árbol Sintáctico:\n" + programaAST.mostrarArbol());
        //falta poner la tabla de símbolos

        guardarTxt("cuadruplos.txt", generador.obtenerCuadruplosEnFormatoTabla());
        guardarTxt("tripletas.txt", generador.obtenerTripletasEnFormatoTabla());
    }//GEN-LAST:event_JMCompilarMouseClicked

    public static String mostrarTablaSimbolos(String codigo) {
        // Analizar el código para generar tokens
        List<Analizador.Token> tokens = Analizador.analizarLexico(codigo);

        // Obtener tabla de símbolos completa
        String tablaCompleta = Analizador.getTablaSimbolosTexto();

        StringBuilder resultado = new StringBuilder();
        resultado.append(tablaCompleta).append("\n\n");

        return resultado.toString();
    }


    private void JMEjecutarMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMEjecutarMouseClicked
        String codigo = JTAEditotText.getText();
        List<Analizador.Token> tokens = Analizador.analizarLexico(codigo);
        StringBuilder errores = new StringBuilder();
        boolean esValido = Analizador.analizarSintaxis(tokens, errores);
        if (!esValido) {
            JTAConsola.setText("❌ No se puede ejecutar. Corrige los errores primero.\n" + errores);
            return;
        }
        List<String> instrucciones = new ArrayList<>();
        String[] lineas = codigo.split("\\n");
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (!limpia.isEmpty() && !limpia.equals("iniciar") && !limpia.equals("fin")) {
                instrucciones.add(limpia);
            }
        }
        JDialog animDialog = new JDialog(this, "Simulación de Robot", true);
        animDialog.setSize(800, 600);
        animDialog.setLocationRelativeTo(this);
        PanelAnimacionRobot panelAnim = new PanelAnimacionRobot(tokens);
        animDialog.getContentPane().add(panelAnim);
        animDialog.setVisible(true);
    }//GEN-LAST:event_JMEjecutarMouseClicked

    private void JMAnalisisSemanticoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMAnalisisSemanticoMouseClicked
        if (generador == null) {
            JOptionPane.showMessageDialog(null,
                    "Es necesario compilar el programa antes");

        }
        JTAConsola.setText("Cuadruplos generados");
        mostrarCuadruplosEnTabla(generador);
    }//GEN-LAST:event_JMAnalisisSemanticoMouseClicked

    private void JMTraducirProgramaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMTraducirProgramaMouseClicked
        String codigoFuenteDePrueba = JTAEditotText.getText();

        try {

            List<Analizador.Token> tokens = Analizador.analizarLexico(codigoFuenteDePrueba);

            // Verificar errores léxicos
            StringBuilder erroresLexicos = new StringBuilder();
            for (Analizador.Token token : tokens) {
                if ("ERROR LEXICO".equals(token.tipo)) {
                    erroresLexicos.append("Error léxico: '").append(token.lexema)
                            .append("' en línea ").append(token.linea).append("\n");
                }
            }
            if (erroresLexicos.length() > 0) {
                JOptionPane.showMessageDialog(this, erroresLexicos.toString(),
                        "Errores Léxicos", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Analizador.EntradaTablaSimbolos> tablaSimbolos = Analizador.getTablaSimbolosCompleta();

            AnalizadorSintactico analizadorSintactico
                    = new AnalizadorSintactico(tokens, tablaSimbolos);

            ProgramaNodo programaAST = analizadorSintactico.parsear(tokens, tablaSimbolos);

            if (analizadorSintactico.getErrores().length() > 0) {
                JOptionPane.showMessageDialog(this, analizadorSintactico.getErrores().toString(),
                        "Errores Sintácticos", JOptionPane.ERROR_MESSAGE);
            } else if (programaAST != null) {

                // --- CÓDIGO INTERMEDIO ---
                GeneradorCodigoIntermedio generadorCodigoIntermedio = new GeneradorCodigoIntermedio();
                programaAST.generaCodigoIntermedio(generadorCodigoIntermedio);
                List<InstruccionTAC> cuadruplos = generadorCodigoIntermedio.getCodigo();

                // --- CÓDIGO OBJETO (ASM) ---
                GeneradorCódigoObjeto generadorEnsamblador
                        = new GeneradorCódigoObjeto(cuadruplos, tablaSimbolos);

                String codigoEnsamblador = generadorEnsamblador.generarCodigo();

                // Mostrar el ASM en ventana emergente
                VentanaCodigoObjeto ventanaASM = new VentanaCodigoObjeto(this, codigoEnsamblador);
                ventanaASM.setVisible(true);

                // Guardar archivo de salida por default
                generadorEnsamblador.guardarEnsambladorEnArchivo("salidaaa.asm");

            } else {
                JOptionPane.showMessageDialog(this,
                        "Error desconocido: No se pudo construir el AST.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "¡Ocurrió un error inesperado durante la compilación!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        }//GEN-LAST:event_JMTraducirProgramaMouseClicked

    private void JMTablaSimbolosMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMTablaSimbolosMouseClicked
// Crear instancia
        GestorTablaSimbolos gestor = new GestorTablaSimbolos();

        gestor.cargarDesdeAnalizador();

        String tabla = gestor.obtenerTablaSimbolosEnFormatoTabla();
// Desde el gestor directamente
        gestor.mostrarTablaEnFrame();


    }//GEN-LAST:event_JMTablaSimbolosMouseClicked

    private void JMAnalsisXSemanticoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMAnalsisXSemanticoMouseClicked
        try {
            // 1️⃣ Obtener el código fuente desde el editor correcto
            String codigoFuente = JTAEditotText.getText().trim();
            if (codigoFuente.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No hay código fuente en el editor.",
                        "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2️⃣ Análisis léxico inicial
            List<Analizador.Token> tokens = Analizador.analizarLexico(codigoFuente);

            // 3️⃣ Verificar errores léxicos (por si el código tiene errores)
            StringBuilder errores = new StringBuilder();
            for (Analizador.Token t : tokens) {
                if ("ERROR LEXICO".equals(t.tipo)) {
                    errores.append("Error léxico: '")
                            .append(t.lexema)
                            .append("' en línea ")
                            .append(t.linea)
                            .append("\n");
                }
            }
            if (errores.length() > 0) {
                JPEditorTextAnalisis.setText("❌ ERRORES LÉXICOS DETECTADOS:\n" + errores);
                return;
            }

            // 4️⃣ Análisis sintáctico y semántico
            AnalizadorSintactico parser = new AnalizadorSintactico(tokens, Analizador.getTablaSimbolosCompleta());
            ProgramaNodo ast = parser.parsear(tokens, Analizador.getTablaSimbolosCompleta());

            if (parser.getErrores() != null && !parser.getErrores().isEmpty()) {
                JPEditorTextAnalisis.setText("❌ ERRORES SEMÁNTICOS DETECTADOS:\n" + parser.getErrores());
                return;
            }

            // 5️⃣ Mostrar los resultados del análisis semántico
            StringBuilder salida = new StringBuilder();
            salida.append("✅ ANÁLISIS SEMÁNTICO CORRECTO\n\n");

            salida.append("═══ TABLA DE SÍMBOLOS ═══\n");
            salida.append(Analizador.getTablaSimbolosTextoConGestor()).append("\n\n");

            salida.append("═══ ÁRBOL DE EXPRESIONES (AST) ═══\n");
            salida.append(generarTextoAST(ast, 0)).append("\n\n");

            salida.append("═══ PILA SEMÁNTICA (Simulación) ═══\n");
            salida.append(simularPilaSemantica(tokens)).append("\n\n");

            salida.append("═══ ACCIONES SEMÁNTICAS APLICADAS ═══\n");
            salida.append("• Comprobación de tipos en expresiones.\n");
            salida.append("• Validación de variables declaradas antes de su uso.\n");
            salida.append("• Prevención de duplicidad de identificadores.\n");
            salida.append("• Verificación de división entre cero.\n");
            salida.append("• Manejo de errores semánticos con mensajes descriptivos.\n");

            // 6️⃣ Mostrar todo en el panel de análisis
            JPEditorTextAnalisis.setText(salida.toString());

        } catch (Exception e) {
            e.printStackTrace();
            JPEditorTextAnalisis.setText("⚠️ Error inesperado durante el análisis semántico:\n" + e.getMessage());
        }
    }//GEN-LAST:event_JMAnalsisXSemanticoMouseClicked

    private void JMCompilacionMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMCompilacionMouseClicked
        try {
            // 1️⃣ Obtener el código fuente del editor
            String codigo = JTAEditotText.getText().trim();
            if (codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No hay código para compilar.",
                        "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2️⃣ Instanciar analizador principal
            Analizador compilador = new Analizador();
            StringBuilder salidaAnalisis = new StringBuilder();
            StringBuilder consola = new StringBuilder();

            consola.append("🚀 INICIANDO COMPILACIÓN...\n");
            consola.append("-------------------------------------------------------------\n");

            // 3️⃣ Análisis Léxico
            List<Analizador.Token> tokens = compilador.analizarLexico(codigo);
            salidaAnalisis.append("🔹 ANÁLISIS LÉXICO\n");
            salidaAnalisis.append("-------------------------------------------------------------\n");

            StringBuilder erroresLexicos = new StringBuilder();
            int lineaActual = -1;
            for (Analizador.Token token : tokens) {
                if (token.linea != lineaActual) {
                    lineaActual = token.linea;
                    salidaAnalisis.append("\n[Línea ").append(lineaActual).append("]\n");
                }
                salidaAnalisis.append(token.tipo)
                        .append(": ")
                        .append(token.lexema)
                        .append(" (col ")
                        .append(token.columna)
                        .append(")\n");

                if ("ERROR LEXICO".equals(token.tipo)) {
                    erroresLexicos.append("Error léxico en '")
                            .append(token.lexema)
                            .append("' línea ")
                            .append(token.linea)
                            .append("\n");
                }
            }

            // Si hay errores léxicos, los mostramos y terminamos
            if (erroresLexicos.length() > 0) {
                consola.append("❌ Se encontraron errores léxicos.\n");
                consola.append(erroresLexicos);
                JTAConsola.setText(consola.toString());
                JPEditorTextAnalisis.setText(salidaAnalisis.toString() + "\n\n" + erroresLexicos);
                return;
            }

            consola.append("✅ Análisis léxico completado correctamente.\n\n");

            // 4️⃣ Análisis Sintáctico
            StringBuilder erroresSintaxis = new StringBuilder();
            boolean esValido = compilador.analizarSintaxis(tokens, erroresSintaxis);

            salidaAnalisis.append("\n\n🔹 ANÁLISIS SINTÁCTICO\n");
            salidaAnalisis.append("-------------------------------------------------------------\n");

            if (!esValido) {
                consola.append("❌ Errores sintácticos detectados.\n");
                String[] lineas = codigo.split("\n");
                String[] mensajes = erroresSintaxis.toString().split("\n");
                StringBuilder salidaErrores = new StringBuilder();
                for (String mensaje : mensajes) {
                    Matcher matcher = Pattern.compile("en línea (\\d+)").matcher(mensaje);
                    if (matcher.find()) {
                        int numLinea = Integer.parseInt(matcher.group(1));
                        String lineaCodigo = (numLinea <= lineas.length) ? lineas[numLinea - 1].trim() : "";
                        salidaErrores.append("L").append(numLinea).append(" | ").append(lineaCodigo).append("\n");
                        salidaErrores.append("    → ").append(mensaje).append("\n\n");
                    } else {
                        salidaErrores.append(mensaje).append("\n");
                    }
                }
                JTAConsola.setText(consola.toString() + salidaErrores);
                JPEditorTextAnalisis.setText(salidaAnalisis.toString() + "\n" + salidaErrores);
                return;
            }

            consola.append("✅ Análisis sintáctico correcto.\n");
            salidaAnalisis.append("Expresiones reconocidas:\n");
            salidaAnalisis.append(Analizador.obtenerPatronesPorLinea(tokens)).append("\n\n");

            // 5️⃣ Tabla de símbolos
            salidaAnalisis.append("🔹 TABLA DE SÍMBOLOS\n");
            salidaAnalisis.append("-------------------------------------------------------------\n");
            salidaAnalisis.append(mostrarTablaSimbolos(codigo)).append("\n\n");

            // 6️⃣ Árbol sintáctico (AST)
            salidaAnalisis.append("🔹 ÁRBOL SINTÁCTICO\n");
            salidaAnalisis.append("-------------------------------------------------------------\n");
            ProgramaNodo arbol = Analizador.parsearAST(codigo, erroresSintaxis);
            salidaAnalisis.append(arbol.mostrarArbol()).append("\n\n");

            consola.append("✅ Árbol sintáctico generado correctamente.\n");

            // 7️⃣ Análisis Semántico
            salidaAnalisis.append("🔹 ANÁLISIS SEMÁNTICO\n");
            salidaAnalisis.append("-------------------------------------------------------------\n");

            AnalizadorSintactico parser = new AnalizadorSintactico(tokens, Analizador.getTablaSimbolosCompleta());
            ProgramaNodo ast = parser.parsear(tokens, Analizador.getTablaSimbolosCompleta());

            if (parser.getErrores() != null && !parser.getErrores().isEmpty()) {
                consola.append("❌ Errores semánticos detectados.\n");
                salidaAnalisis.append(parser.getErrores());
                JTAConsola.setText(consola.toString());
                JPEditorTextAnalisis.setText(salidaAnalisis.toString());
                return;
            }

            salidaAnalisis.append("✅ Análisis semántico correcto.\n\n");
            salidaAnalisis.append("Tabla de símbolos final:\n");
            salidaAnalisis.append(Analizador.getTablaSimbolosTextoConGestor()).append("\n\n");

            consola.append("✅ Análisis semántico completado.\n");

            // 8️⃣ Guardar archivo compilado si todo fue correcto
            consola.append("💾 Guardando archivo compilado...\n");
            if (archivoActual != null) {
                try (FileWriter writer = new FileWriter(archivoActual)) {
                    writer.write(codigo);
                    consola.append("Archivo guardado exitosamente en: " + archivoActual.getAbsolutePath() + "\n");
                } catch (IOException e) {
                    consola.append("⚠️ Error al guardar el archivo: " + e.getMessage() + "\n");
                }
            } else {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar archivo compilado");
                int userSelection = fileChooser.showSaveDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    archivoActual = fileChooser.getSelectedFile();
                    try (FileWriter writer = new FileWriter(archivoActual)) {
                        writer.write(codigo);
                        consola.append("Archivo guardado exitosamente en: " + archivoActual.getAbsolutePath() + "\n");
                        cargarArchivosEnPanel(archivoActual.getParentFile());
                    } catch (IOException e) {
                        consola.append("⚠️ Error al guardar el archivo: " + e.getMessage() + "\n");
                    }
                } else {
                    consola.append("⛔ Guardado cancelado por el usuario.\n");
                }
            }

            consola.append("-------------------------------------------------------------\n");
            consola.append("✅ COMPILACIÓN FINALIZADA SIN ERRORES.\n");

            // 9️⃣ Mostrar resultados
            JTAConsola.setText(consola.toString());
            JPEditorTextAnalisis.setText(salidaAnalisis.toString());

        } catch (Exception e) {
            JTAConsola.setText("⚠️ Error durante la compilación: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_JMCompilacionMouseClicked

    private void JMOptimizadorMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_JMOptimizadorMouseClicked
        try {

            // 1️⃣ TAC SIN OPTIMIZAR
            List<InstruccionTAC> codigoSinOpt = generador.getCodigoSinOptimizar();

            // 2️⃣ TAC OPTIMIZADO
            List<InstruccionTAC> codigoOpt = generador.getCodigo();

            // Crear panel con pestañas
            JTabbedPane tabs = new JTabbedPane();

            // ====================================================
            // 1) CÓDIGO INTERMEDIO (CUÁDRUPLOS) SIN OPTIMIZAR
            // ====================================================
            StringBuilder sb1 = new StringBuilder();
            sb1.append("===== CÓDIGO INTERMEDIO (SIN OPTIMIZAR) =====\n\n");
            for (InstruccionTAC tac : codigoSinOpt) {
                sb1.append(tac.toString()).append("\n");
            }
            JTextArea txt1 = new JTextArea(sb1.toString(), 25, 60);
            txt1.setEditable(false);
            tabs.addTab("Cuádruplos", new JScrollPane(txt1));

            // ====================================================
            // 2) CÓDIGO INTERMEDIO (CUÁDUPLOS) OPTIMIZADO
            // ====================================================
            StringBuilder sb2 = new StringBuilder();
            sb2.append("===== CÓDIGO INTERMEDIO (OPTIMIZADO) =====\n\n");
            for (InstruccionTAC tac : codigoOpt) {
                sb2.append(tac.toString()).append("\n");
            }
            JTextArea txt2 = new JTextArea(sb2.toString(), 25, 60);
            txt2.setEditable(false);
            tabs.addTab("Cuádruplos Opt.", new JScrollPane(txt2));

            // ====================================================
            // 3) TRIPLETAS (SIN OPTIMIZAR)
            // ====================================================
            StringBuilder sb3 = new StringBuilder();
            sb3.append("===== TRIPLETAS (SIN OPTIMIZAR) =====\n\n");

            int id = 0;
            for (InstruccionTAC tac : codigoSinOpt) {
                sb3.append(id++)
                        .append(")  ")
                        .append((tac.getOperacion() != null ? tac.getOperacion() : ""))
                        .append("   ")
                        .append((tac.getArg1() != null ? tac.getArg1() : ""))
                        .append("   ")
                        .append((tac.getArg2() != null ? tac.getArg2() : ""))
                        .append("\n");
            }

            JTextArea txt3 = new JTextArea(sb3.toString(), 25, 60);
            txt3.setEditable(false);
            tabs.addTab("Tripletas", new JScrollPane(txt3));

            // ====================================================
            // 4) TRIPLETAS (OPTIMIZADAS)
            // ====================================================
            StringBuilder sb4 = new StringBuilder();
            sb4.append("===== TRIPLETAS (OPTIMIZADAS) =====\n\n");

            id = 0;
            for (InstruccionTAC tac : codigoOpt) {
                sb4.append(id++)
                        .append(")  ")
                        .append((tac.getOperacion() != null ? tac.getOperacion() : ""))
                        .append("   ")
                        .append((tac.getArg1() != null ? tac.getArg1() : ""))
                        .append("   ")
                        .append((tac.getArg2() != null ? tac.getArg2() : ""))
                        .append("\n");
            }

            JTextArea txt4 = new JTextArea(sb4.toString(), 25, 60);
            txt4.setEditable(false);
            tabs.addTab("Tripletas Opt.", new JScrollPane(txt4));

            // Mostrar ventana final con pestañas
            JOptionPane.showMessageDialog(
                    this,
                    tabs,
                    "OPTIMIZACIÓN DE CÓDIGO INTERMEDIO",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error al generar optimización: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }//GEN-LAST:event_JMOptimizadorMouseClicked

    private String generarTextoAST(Object nodo, int nivel) {
        StringBuilder sb = new StringBuilder();
        if (nodo == null) {
            return "";
        }

        String sangria = "  ".repeat(nivel);
        sb.append(sangria)
                .append("• ")
                .append(nodo.getClass().getSimpleName());

        String detalle = nodo.toString();
        if (detalle != null && !detalle.equals(nodo.getClass().getName())) {
            sb.append(" → ").append(detalle);
        }

        sb.append("\n");

        try {
            for (java.lang.reflect.Field campo : nodo.getClass().getDeclaredFields()) {
                campo.setAccessible(true);
                Object valor = campo.get(nodo);
                if (valor instanceof java.util.List<?>) {
                    for (Object subnodo : (java.util.List<?>) valor) {
                        if (subnodo != null && subnodo.getClass().getSimpleName().endsWith("Nodo")) {
                            sb.append(generarTextoAST(subnodo, nivel + 1));
                        }
                    }
                } else if (valor != null && valor.getClass().getSimpleName().endsWith("Nodo")) {
                    sb.append(generarTextoAST(valor, nivel + 1));
                }
            }
        } catch (Exception e) {
            // ignorar campos inaccesibles
        }

        return sb.toString();
    }

    private String simularPilaSemantica(List<Analizador.Token> tokens) {
        StringBuilder pila = new StringBuilder();
        java.util.Stack<String> stack = new java.util.Stack<>();

        for (Analizador.Token t : tokens) {
            switch (t.tipo) {
                case "NUMERO":
                case "IDENTIFICADOR":
                    stack.push(t.lexema);
                    pila.append("PUSH(").append(t.lexema).append(")\n");
                    break;

                case "OPERADOR_ARITMETICO":
                    if (stack.size() >= 2) {
                        String op2 = stack.pop();
                        String op1 = stack.pop();
                        String temp = "(" + op1 + " " + t.lexema + " " + op2 + ")";
                        stack.push(temp);
                        pila.append("POP(").append(op1).append(", ").append(op2)
                                .append(") → PUSH(").append(temp).append(")\n");
                    }
                    break;
            }
        }

        pila.append("\nTope de pila: ").append(stack.isEmpty() ? "[vacía]" : stack.peek()).append("\n");
        return pila.toString();
    }

    private void mostrarManual() {
        String manual = """
        #📘 MANUAL DE MINI-ROBOT-CODE
        
        ## ▸ ESTRUCTURA BÁSICA:
        Todo programa debe iniciar con la palabra clave 'iniciar' y finalizar con 'Terminar' (con T mayúscula).
        Estas indican el inicio y el fin del código del robot.
        
        **Gramática:** `<programa> ::= "iniciar" <declaraciones> <instrucciones> "Terminar"`
        
        **Ejemplo:**
        
        iniciar
            ...declaraciones...
            ...instrucciones...
        Terminar
        
        
        ## ▸ DECLARACIONES DE VARIABLES:
        Antes de usar variables, deben declararse especificando su tipo.
        
        Variables numéricas:
        
        <identificador> tipo numero = <expresion>
        
        
        Sensores:
        
        <identificador> tipo Sensor = <puerto>
        
        
        Gramática:
        <declaracion_numero> ::= <identificador> "tipo" "numero" "=" <expresion>
        <declaracion_sensor> ::= <identificador> "tipo" "Sensor" "=" <numero>
        
        Ejemplos:
        
        velocidad tipo numero = 10
        contador tipo numero = 5 + 3
        sensor1 tipo Sensor = 1
        sensor_distancia tipo Sensor = 2
        
        
        ## ▸ EXPRESIONES ARITMÉTICAS:
        Puedes realizar cálculos matemáticos con precedencia de operadores.
        
        Operadores: +, -, *, /  
        Precedencia: () > *, / > +, -
        
        Gramática:
        <expresion> ::= <termino> | <expresion> ("+" | "-") <termino>
        <termino> ::= <factor> | <termino> ("*" | "/") <factor>
        <factor> ::= <numero> | <identificador> | "(" <expresion> ")" | <leer_sensor>
        
        Ejemplos:
        
        resultado tipo numero = (5 + 3) * 2
        distancia tipo numero = velocidad / 2
        calculo tipo numero = leer_sensor sensor1 + 10
        
        
        ## ▸ MOVIMIENTOS:
        Comandos que mueven o rotan al robot. Aceptan expresiones como parámetros.
        
        Comandos disponibles:
        mover_adelante <expresion>` → Avanza hacia adelante
        mover_atras <expresion>` → Retrocede  
        girar_izquierda <expresion>` → Gira a la izquierda (grados)
        girar_derecha <expresion>` → Gira a la derecha (grados)
        
        Gramática:
        <comando_movimiento> ::= ("mover_adelante" | "mover_atras" | "girar_izquierda" | "girar_derecha") <expresion>
        
        Ejemplos:
        
        mover_adelante 10
        mover_adelante velocidad
        girar_derecha velocidad * 2
        mover_atras leer_sensor sensor1
        
        
        ## ▸ ACTUADORES:
        Controlan salidas como LEDs. Requieren un identificador de color.
        
        Comandos disponibles:
        encender_led <identificador>` → Enciende el LED del color indicado
        apagar_led <identificador>` → Apaga el LED del color indicado
        
        Gramática:
       <comando_actuador> ::= ("encender_led" | "apagar_led") <identificador>
        
        Colores válidos: rojo, verde, azul, amarillo, blanco
        
        Ejemplos:
        
        encender_led rojo
        apagar_led verde
        encender_led amarillo
        
        
        ## ▸ TIEMPO:
        El comando 'esperar' pausa temporalmente el robot.
        
        Sintaxis: 
        esperar <expresion> <unidad>`
        <expresion>: cantidad de tiempo (puede ser un cálculo)
        <unidad>: segundos | milisegundos
        
        Gramática: 
        <comando_tiempo> ::= "esperar" <expresion> <unidad_tiempo>
        
        Ejemplos:
        
        esperar 2 segundos
        esperar 500 milisegundos
        esperar velocidad * 100 milisegundos
        esperar leer_sensor sensor1 segundos
        
        
        ## ▸ SENSORES:
        Leen valores del hardware del robot para usarse en cálculos y condiciones.
        
        Sintaxis: leer_sensor <identificador>
        
        Gramática: 
        <leer_sensor> ::= "leer_sensor" <identificador>
        
        Ejemplo:
        
        sensor_frontal tipo Sensor = 1
        si leer_sensor sensor_frontal > 20 entonces
            mover_adelante 5
        fin si
        
        
        ## ▸ ASIGNACIONES:
        Modifican el valor de variables ya declaradas.
        
        Sintaxis: `identificador> = <expresion>
        
        Gramática: 
        <asignacion> ::= <identificador> "=" <expresion>
        
        Ejemplos:
        
        contador = contador + 1
        velocidad = leer_sensor sensor1 * 2
        resultado = (a + b) / 2
        
        
        ## ▸ ESTRUCTURAS DE CONTROL:
        
        ### Condicionales (si-entonces):
        Ejecuta código solo si se cumple una condición.
        
        Sintaxis:
        
        si <condicion> entonces
            <instrucciones>
        fin si
        
        
        Gramática: <bloque_si> ::= "si" <condicion> "entonces" <instrucciones> "fin si"
        
        Operadores relacionales: <, >, <=, >=, ==, !=
        
        Gramática de condición: <condicion> ::= <expresion> <operador_relacional> <expresion>
        
        ### Bucles para:
        Repite código un número determinado de veces.
        
        Sintaxis:
        
        para <variable> = <inicio> hasta <fin> hacer
            <instrucciones>
        fin para
        
        
        Gramática: 
        <bloque_para> ::= "para" <identificador> "=" <expresion> "hasta" <expresion> "hacer" <instrucciones> "fin para"
        
        ### Bucles mientras:
        Repite código mientras se cumpla una condición.
        
        Sintaxis:
        
        mientras <condicion> hacer
            <instrucciones>
        fin mientras  
        
        
        Gramática: <bloque_mientras> ::= "mientras" <condicion> "hacer" <instrucciones> "fin mientras"
        
        Ejemplos de estructuras:
        
        si velocidad > 10 entonces
            encender_led verde
            mover_adelante velocidad / 2
        fin si
        
        para i = 1 hasta 5 hacer
            girar_derecha 72
            mover_adelante 10
        fin para
        
        mientras leer_sensor sensor1 < 50 hacer
            mover_adelante 5
            esperar 100 milisegundos
        fin mientras
        
        
        ## ▸ COMANDOS DE CONTROL DE FLUJO:
        romper → Sale del bucle actual
        detener → Detiene TODAS las operaciones
        
        Gramática: <comando_control> ::= "romper" | "detener"
        
        ## ▸ COMENTARIOS:
        Agregan documentación al código sin afectar la ejecución.
        
        Sintaxis: // comentario o # comentario
        
        Ejemplos
                         
        // Este es un comentario de línea
        velocidad tipo numero = 10  // Comentario al final de línea
        # También puedes usar numeral
        
        
        ## ▸ REGLAS IMPORTANTES:
        - Usa siempre 'iniciar' al principio y 'Terminar' al final del programa
        - Declara todas las variables antes de usarlas
        - Cada instrucción debe ir en una línea separada
        - Cierra correctamente todos los bloques: 'fin si', 'fin para', 'fin mientras'
        - Los nombres de variables deben empezar con letra o guión bajo
        - Las expresiones pueden anidarse con paréntesis para cambiar precedencia
        
        ## ▸ EJEMPLO COMPLETO:
        ```
        iniciar
            // Declaraciones
            velocidad tipo numero = 15
            sensor_frontal tipo Sensor = 1
            contador tipo numero = 0
            
            // Bucle principal
            para i = 1 hasta 3 hacer
                contador = contador + 1
                si leer_sensor sensor_frontal > 20 entonces
                    encender_led verde
                    mover_adelante velocidad
                    esperar 1 segundos
                fin si
                girar_derecha 120
            fin para
            
            // Finalización
            apagar_led verde
            detener
        Terminar
        
        
        ## ▸ GRAMÁTICA COMPLETA DEL LENGUAJE:
        
        <programa> ::= "iniciar" <declaraciones> <instrucciones> "Terminar"
        
        <declaraciones> ::= <declaracion> <declaraciones> | ε
        <declaracion> ::= <declaracion_numero> | <declaracion_sensor>
        <declaracion_numero> ::= <identificador> "tipo" "numero" "=" <expresion>
        <declaracion_sensor> ::= <identificador> "tipo" "Sensor" "=" <numero>
        
        <instrucciones> ::= <instruccion> <instrucciones> | ε
        <instruccion> ::= <comando_movimiento> | <comando_actuador> | <comando_tiempo> | 
                          <estructura_control> | <asignacion> | <comando_control>
        
        <comando_movimiento> ::= ("mover_adelante" | "mover_atras" | "girar_izquierda" | "girar_derecha") <expresion>
        <comando_actuador> ::= ("encender_led" | "apagar_led") <identificador>
        <comando_tiempo> ::= "esperar" <expresion> <unidad_tiempo>
        <comando_control> ::= "romper" | "detener"
        
        <estructura_control> ::= <bloque_si> | <bloque_para> | <bloque_mientras>
        <bloque_si> ::= "si" <condicion> "entonces" <instrucciones> "fin si"
        <bloque_para> ::= "para" <identificador> "=" <expresion> "hasta" <expresion> "hacer" <instrucciones> "fin para"
        <bloque_mientras> ::= "mientras" <condicion> "hacer" <instrucciones> "fin mientras"
        
        <condicion> ::= <expresion> <operador_relacional> <expresion>
        <operador_relacional> ::= "<" | ">" | "<=" | ">=" | "==" | "!="
        
        <asignacion> ::= <identificador> "=" <expresion>
        
        <expresion> ::= <termino> | <expresion> ("+" | "-") <termino>
        <termino> ::= <factor> | <termino> ("*" | "/") <factor>
        <factor> ::= <numero> | <identificador> | "(" <expresion> ")" | <leer_sensor>
        
        <leer_sensor> ::= "leer_sensor" <identificador>
        <unidad_tiempo> ::= "segundos" | "milisegundos"
        <numero> ::= [0-9]+(.[0-9]+)?                
        <identificador> ::= [a-zA-Z_][a-zA-Z_0-9]*
       
        
      
        
        Revisa tu código antes de ejecutar. Errores comunes:
        - Olvidar declarar variables antes de usarlas
        - No cerrar bloques con 'fin si', 'fin para', o 'fin mientras'  
        - Usar 'fin' en lugar de 'Terminar' al final del programa
        - Comandos sin parámetros o parámetros incorrectos
        - Paréntesis desbalanceados en expresiones
        
        Para soporte técnico: +52 334 410 54 27
        """;

        JTextArea area = new JTextArea(manual);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        JDialog dialogo = new JDialog(this, "Guía del Lenguaje MINI-ROBOT-CODE", true);
        dialogo.setSize(600, 500);
        dialogo.setLocationRelativeTo(this);
        dialogo.add(scroll);
        dialogo.setVisible(true);
    }

    private void cargarArchivosEnPanel(File carpeta) {
        JPCarpeta.removeAll();
        JPCarpeta.setLayout(new BoxLayout(JPCarpeta, BoxLayout.Y_AXIS));
        JPCarpeta.add(btnCambiarCarpeta);
        JPCarpeta.add(Box.createVerticalStrut(2));
        carpetaActual = carpeta;
        File[] archivos = carpeta.listFiles((dir, name) -> name.endsWith(".txt"));
        if (archivos != null) {
            for (File archivo : archivos) {
                JLabel label = new JLabel(archivo.getName());
                label.setOpaque(true);
                label.setBackground(archivo.equals(archivoActual) ? Color.BLUE : Color.WHITE);
                label.setForeground(archivo.equals(archivoActual) ? Color.WHITE : Color.BLACK);
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {
                            mostrarMenuContextual(evt, archivo);
                        } else if (javax.swing.SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 1) {
                            try {
                                archivoActual = archivo;
                                String contenido = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8);
                                JTAEditotText.setText(contenido);
                                cargarArchivosEnPanel(carpeta);
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null, "Error al abrir el archivo");
                            }
                        }
                    }
                });
                JPCarpeta.add(label);
            }
        }
        JPCarpeta.revalidate();
        JPCarpeta.repaint();
    }

    private void configurarUndoRedo() {
        JTAEditotText.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });
        JTAEditotText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 && e.getKeyCode() == KeyEvent.VK_Z) {
                    try {
                        if (undoManager.canUndo()) {
                            undoManager.undo();
                        }
                    } catch (CannotUndoException ex) {
                        System.out.println("No se puede deshacer: " + ex);
                    }
                }
            }
        });
    }

    private void mostrarContenidoAnalisis() {
        String contenido = JPEditorTextAnalisis.getText();
        JEditorPane area = new JEditorPane();
        area.setContentType("text/plain");
        area.setText(contenido);
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        JDialog dialogo = new JDialog(this, "Vista del Panel de Análisis", true);
        dialogo.setSize(1100, 700);
        dialogo.setLocationRelativeTo(this);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.getContentPane().add(scroll);
        dialogo.setVisible(true);
    }

    private void mostrarContenidoConsola() {
        String contenido = JTAConsola.getText();
        JEditorPane area = new JEditorPane();
        area.setContentType("text/plain");
        area.setText(contenido);
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        JDialog dialogo = new JDialog(this, "Vista del Panel de Análisis", true);
        dialogo.setSize(1100, 700);
        dialogo.setLocationRelativeTo(this);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.getContentPane().add(scroll);
        dialogo.setVisible(true);
    }

    private void mostrarMenuContextual(MouseEvent evt, File archivo) {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem copiar = new javax.swing.JMenuItem("Copiar");
        copiar.addActionListener(e -> archivoCopiado = archivo);
        javax.swing.JMenuItem pegar = new javax.swing.JMenuItem("Pegar");
        pegar.addActionListener(e -> {
            if (archivoCopiado != null && archivoCopiado.exists()) {
                try {
                    File destino = new File(archivo.getParent(), archivoCopiado.getName());
                    if (destino.exists()) {
                        JOptionPane.showMessageDialog(this, "Ya existe un archivo con ese nombre");
                        return;
                    }
                    Files.copy(archivoCopiado.toPath(), destino.toPath());
                    cargarArchivosEnPanel(archivo.getParentFile());
                    JTAConsola.setText("Archivo pegado en: " + destino.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error al pegar el archivo");
                }
            }
        });
        javax.swing.JMenuItem duplicar = new javax.swing.JMenuItem("Duplicar");
        duplicar.addActionListener(e -> {
            try {
                File dir = archivo.getParentFile();
                String baseName = archivo.getName().replaceFirst("[.][^.]+$", "");
                String extension = archivo.getName().substring(archivo.getName().lastIndexOf('.'));
                int contador = 1;
                File nuevoArchivo;
                do {
                    nuevoArchivo = new File(dir, baseName + " -Copia" + (contador > 1 ? " (" + contador + ")" : "") + extension);
                    contador++;
                } while (nuevoArchivo.exists());
                Files.copy(archivo.toPath(), nuevoArchivo.toPath());
                cargarArchivosEnPanel(dir);
                JTAConsola.setText("Archivo duplicado como: " + nuevoArchivo.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al duplicar el archivo");
            }
        });
        javax.swing.JMenuItem propiedades = new javax.swing.JMenuItem("Propiedades");
        propiedades.addActionListener(e -> {
            String info = "Nombre: " + archivo.getName() + "\n"
                    + "Ruta: " + archivo.getAbsolutePath() + "\n"
                    + "Tamaño: " + archivo.length() + " bytes\n"
                    + "Última modificación: " + new java.util.Date(archivo.lastModified());
            JOptionPane.showMessageDialog(this, info, "Propiedades del archivo", JOptionPane.INFORMATION_MESSAGE);
        });
        javax.swing.JMenuItem eliminar = new javax.swing.JMenuItem("Eliminar");
        eliminar.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Seguro que deseas eliminar el archivo?\n" + archivo.getName(),
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (archivo.delete()) {
                    cargarArchivosEnPanel(archivo.getParentFile());
                    JTAConsola.setText("Archivo eliminado: " + archivo.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar el archivo");
                }
            }
        });
        menu.add(copiar);
        menu.add(pegar);
        menu.add(duplicar);
        menu.add(eliminar);
        menu.addSeparator();
        menu.add(propiedades);
        menu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void mostrarMenuPegarEnPanel(MouseEvent evt) {
        if (archivoCopiado != null && carpetaActual != null && carpetaActual.isDirectory()) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem pegar = new JMenuItem("Pegar");
            pegar.addActionListener(e -> {
                try {
                    File destino = new File(carpetaActual, archivoCopiado.getName());
                    if (destino.exists()) {
                        String nombre = archivoCopiado.getName();
                        String base = nombre.replaceFirst("[.][^.]+$", "");
                        String ext = nombre.substring(nombre.lastIndexOf('.'));
                        int contador = 1;
                        File nuevo;
                        do {
                            nuevo = new File(carpetaActual, base + " -Copia" + (contador > 1 ? " (" + contador + ")" : "") + ext);
                            contador++;
                        } while (nuevo.exists());
                        destino = nuevo;
                    }
                    Files.copy(archivoCopiado.toPath(), destino.toPath());
                    cargarArchivosEnPanel(carpetaActual);
                    JTAConsola.setText("Archivo pegado: " + destino.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error al pegar el archivo");
                }
            });
            menu.add(pegar);
            menu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /////////////////////////////clase para la tabla cuadruplos
        private class CuadruploTableModel extends AbstractTableModel {

        private final String[] columnNames = {"#", "Operacion", "Argumento 1", "Argumento 2", "Resultado"};
        private List<InstruccionTAC> data;

        public CuadruploTableModel(List<InstruccionTAC> cuadruplos) {
            this.data = cuadruplos;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            InstruccionTAC cuadruplo = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return rowIndex + 1;
                case 1:
                    return cuadruplo.getOperacion();
                case 2:
                    return cuadruplo.getArg1();
                case 3:
                    return cuadruplo.getArg2();
                case 4:
                    return cuadruplo.getResultado();
                default:
                    return null;
            }
        }
    }

    ///////////////////////clase para tripletas
        private class TripleteTableModel extends AbstractTableModel {

        private final String[] columnNames = {"#", "Operacion", "Argumento 1", "Argumento 2"};
        private List<InstruccionTAC> data;

        public TripleteTableModel(List<InstruccionTAC> tripletes) {
            this.data = tripletes;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            InstruccionTAC triplete = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return rowIndex + 1;
                case 1:
                    return triplete.getOperacion();
                case 2:
                    return triplete.getArg1();
                case 3:
                    return triplete.getArg2();
                default:
                    return null;
            }
        }
    }

    // Método para personalizar el aspecto de las tablas
    private void personalizarTabla(JTable table) {
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.setFont(new Font("Monospaced", Font.PLAIN, 14));
        table.setGridColor(Color.LIGHT_GRAY);
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.PINK);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
    }

    private void mostrarCuadruplosEnTabla(GeneradorCodigoIntermedio generador) {
        //frame
        JFrame frame = new JFrame("Cuadruplos y Tripletas Generados");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1200, 600);
        frame.setLayout(new BorderLayout());

        ////pesta;a dise;o
        JTabbedPane tabbedPane = new JTabbedPane();

        //cuadruplosss
        JPanel cuadruplosPanel = new JPanel(new BorderLayout());
        JTable cuadruplosTable = new JTable(new CuadruploTableModel(generador.getCodigo()));
        personalizarTabla(cuadruplosTable);
        cuadruplosPanel.add(new JScrollPane(cuadruplosTable), BorderLayout.CENTER);
        tabbedPane.addTab("Cuadruplos", cuadruplosPanel);

        //tripletassssa
        JPanel tripletasPanel = new JPanel(new BorderLayout());
        JTable tripletasTable = new JTable(new TripleteTableModel(generador.getCodigo()));
        personalizarTabla(tripletasTable);
        tripletasPanel.add(new JScrollPane(tripletasTable), BorderLayout.CENTER);
        tabbedPane.addTab("Tripletas", tripletasPanel);

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(INTERFAZ.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(INTERFAZ.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(INTERFAZ.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(INTERFAZ.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new INTERFAZ().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar JBMBarra;
    private javax.swing.JCheckBoxMenuItem JCBAbrir;
    private javax.swing.JCheckBoxMenuItem JCBAyuda;
    private javax.swing.JCheckBoxMenuItem JCBGuardar;
    private javax.swing.JCheckBoxMenuItem JCBNuevo;
    private javax.swing.JMenu JMAnalisisLexico;
    private javax.swing.JMenu JMAnalisisSemantico;
    private javax.swing.JMenu JMAnalsisXSemantico;
    private javax.swing.JMenu JMCompilacion;
    private javax.swing.JMenu JMCompilar;
    private javax.swing.JMenu JMEjecutar;
    private javax.swing.JMenu JMOptimizador;
    private javax.swing.JMenu JMSintactico;
    private javax.swing.JMenu JMTablaSimbolos;
    private javax.swing.JMenu JMTraducirPrograma;
    private javax.swing.JPanel JPAnalisis;
    private javax.swing.JPanel JPCarpeta;
    private javax.swing.JPanel JPConsola;
    private javax.swing.JEditorPane JPEditorTextAnalisis;
    private javax.swing.JPanel JPEditorTexto;
    private javax.swing.JTextPane JTAConsola;
    private javax.swing.JTextPane JTAEditotText;
    private javax.swing.JButton btnCambiarCarpeta;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    // End of variables declaration//GEN-END:variables

    public void guardarTxt(String nombreArchivo, String datos) {
        try (FileWriter writer = new FileWriter(nombreArchivo)) {
            writer.write(datos);
            System.out.println("Txt generado y guardado en: " + nombreArchivo);
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo: " + e.getMessage());
        }
    }
}
