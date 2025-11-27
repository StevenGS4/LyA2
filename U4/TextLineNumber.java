import java.awt.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class TextLineNumber extends JPanel {
    private final JTextPane textPane;
    private final FontMetrics fontMetrics;
    private final int padding = 8;
    private int lastLineCount = 0;
    
    public TextLineNumber(JTextPane textPane) {
        this.textPane = textPane;
        
        // Configurar la fuente
        Font editorFont = textPane.getFont();
        Font lineNumberFont = new Font("Monospaced", Font.PLAIN, editorFont.getSize());
        setFont(lineNumberFont);
        fontMetrics = getFontMetrics(lineNumberFont);
        
        // Configurar el tamaño y apariencia
        setPreferredSize(new Dimension(50, Integer.MAX_VALUE));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        setOpaque(true);
        
        // Listeners para actualizar cuando cambie el contenido
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    updateSizeAndRepaint();
                });
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    updateSizeAndRepaint();
                });
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> repaint());
            }
        });
        
        // Listener para el cursor
        textPane.addCaretListener(e -> repaint());
        
        // Listener para cambios de tamaño del componente
        textPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    updateSizeAndRepaint();
                });
            }
        });
        
        // Listener para cambios de fuente
        textPane.addPropertyChangeListener("font", evt -> {
            updateFont();
        });
        
        // Sincronizar con el scroll del JTextPane
        JScrollPane scrollPane = getScrollPane();
        if (scrollPane != null) {
            scrollPane.getViewport().addChangeListener(e -> repaint());
        }
    }
    
    private JScrollPane getScrollPane() {
        Container parent = textPane.getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane) {
                return (JScrollPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    private void updateSizeAndRepaint() {
        int currentLineCount = textPane.getDocument().getDefaultRootElement().getElementCount();
        
        // Solo actualizar si el número de líneas cambió
        if (currentLineCount != lastLineCount) {
            lastLineCount = currentLineCount;
            updateSize();
        }
        
        repaint();
    }
    
    private void updateSize() {
        try {
            // Calcular el ancho necesario basado en el número de líneas
            int totalLines = textPane.getDocument().getDefaultRootElement().getElementCount();
            String maxLineNumber = String.valueOf(totalLines);
            int width = fontMetrics.stringWidth(maxLineNumber) + (padding * 2);
            
            Dimension newSize = new Dimension(width, getHeight());
            if (!newSize.equals(getPreferredSize())) {
                setPreferredSize(new Dimension(width, Integer.MAX_VALUE));
                revalidate();
            }
        } catch (Exception e) {
            // Ignorar errores de cálculo de tamaño
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Activar antialiasing para mejor calidad de texto
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        try {
            paintLineNumbers(g2d);
        } catch (Exception e) {
            System.err.println("Error en paintComponent: " + e.getMessage());
        } finally {
            g2d.dispose();
        }
    }
    
    private void paintLineNumbers(Graphics2D g2d) throws BadLocationException {
        // Configurar color y fuente
        g2d.setColor(new Color(128, 128, 128));
        g2d.setFont(getFont());
        
        // Obtener información del área visible
        Rectangle clipBounds = g2d.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, getWidth(), getHeight());
        }
        
        // Obtener el área visible del JTextPane
        Rectangle visibleRect = textPane.getVisibleRect();
        
        // Calcular las líneas que están visibles
        Point startPoint = new Point(0, visibleRect.y);
        Point endPoint = new Point(0, visibleRect.y + visibleRect.height);
        
        int startOffset = textPane.viewToModel2D(startPoint);
        int endOffset = textPane.viewToModel2D(endPoint);
        
        Element root = textPane.getDocument().getDefaultRootElement();
        int startLine = root.getElementIndex(startOffset);
        int endLine = root.getElementIndex(endOffset);
        
        // Asegurar que no excedamos el número total de líneas
        int totalLines = root.getElementCount();
        endLine = Math.min(endLine, totalLines - 1);
        
        // Dibujar los números de línea solo para las líneas visibles
        for (int lineNumber = startLine; lineNumber <= endLine; lineNumber++) {
            Element lineElement = root.getElement(lineNumber);
            int startOffsetLine = lineElement.getStartOffset();
            
            try {
                // Obtener la posición de la línea en el JTextPane
                Rectangle2D lineRect = textPane.modelToView2D(startOffsetLine);
                
                if (lineRect != null) {
                    // Calcular la posición Y en el panel de números de línea
                    int y = (int) (lineRect.getY() - visibleRect.y + fontMetrics.getAscent());
                    
                    // Solo dibujar si está dentro del área visible
                    if (y >= clipBounds.y - fontMetrics.getHeight() && 
                        y <= clipBounds.y + clipBounds.height + fontMetrics.getHeight()) {
                        
                        String lineNumberStr = String.valueOf(lineNumber + 1);
                        int x = getWidth() - fontMetrics.stringWidth(lineNumberStr) - padding;
                        
                        g2d.drawString(lineNumberStr, x, y);
                    }
                }
            } catch (BadLocationException e) {
                // Ignorar líneas que no se pueden procesar
                continue;
            }
        }
    }
    
    // Método para actualizar cuando cambie la fuente del editor
    public void updateFont() {
        Font editorFont = textPane.getFont();
        Font lineNumberFont = new Font("Monospaced", Font.PLAIN, editorFont.getSize());
        setFont(lineNumberFont);
        updateSize();
        repaint();
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // Asegurar que el alto coincida con el del JTextPane
        if (textPane != null) {
            size.height = textPane.getPreferredSize().height;
        }
        return size;}}