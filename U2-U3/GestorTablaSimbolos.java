
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GestorTablaSimbolos {
    private List<EntradaTablaSimbolos> tablaSimbolos;
    
    public GestorTablaSimbolos() {
        this.tablaSimbolos = new ArrayList<>();
    }
    
    // Método para agregar una nueva entrada a la tabla
    public void agregarSimbolo(String nombre, String tipo, int linea, int columna) {
        if (!existeSimbolo(nombre)) {
            tablaSimbolos.add(new EntradaTablaSimbolos(nombre, tipo, linea, columna));
        }
    }
    
    static class VentanaTablaSimbolos extends javax.swing.JFrame {
        private javax.swing.JTable tabla;
        private javax.swing.table.DefaultTableModel modeloTabla;
        private javax.swing.JLabel lblEstadisticas;
        private GestorTablaSimbolos gestor;
        
        public VentanaTablaSimbolos(GestorTablaSimbolos gestor) {
            this.gestor = gestor;
            initComponents();
            cargarDatos();
        }
        
        private void initComponents() {
            setTitle("Tabla de Símbolos");
            setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            setLayout(new java.awt.BorderLayout());
            
            // Panel superior con estadísticas
            lblEstadisticas = new javax.swing.JLabel();
            lblEstadisticas.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 5, 10));
            add(lblEstadisticas, java.awt.BorderLayout.NORTH);
            
            // Crear tabla
            String[] columnas = {"Variable", "Tipo", "Línea", "Columna"};
            modeloTabla = new javax.swing.table.DefaultTableModel(columnas, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            tabla = new javax.swing.JTable(modeloTabla);
            tabla.setRowHeight(25);
            
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(tabla);
            scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
            add(scrollPane, java.awt.BorderLayout.CENTER);
            
            // Panel de botones
            javax.swing.JPanel panelBotones = new javax.swing.JPanel();
            javax.swing.JButton btnCerrar = new javax.swing.JButton("Cerrar");
            
            btnCerrar.addActionListener(e -> dispose());
            
            panelBotones.add(btnCerrar);
            add(panelBotones, java.awt.BorderLayout.SOUTH);
            
            pack();
            setLocationRelativeTo(null);
        }
        
        private void cargarDatos() {
            modeloTabla.setRowCount(0);
            
            if (gestor.estaVacia()) {
                lblEstadisticas.setText("No hay símbolos en la tabla");
                return;
            }
            
            for (EntradaTablaSimbolos entrada : gestor.getTablaSimbolos()) {
                Object[] fila = {
                    entrada.getNombre(),
                    entrada.getTipo(),
                    entrada.getLineaDeclaracion(),
                    entrada.getColumnaDeclaracion()
                };
                modeloTabla.addRow(fila);
            }
            
            lblEstadisticas.setText("Total de símbolos: " + gestor.getTamaño());
        }
    }
    
    // Método para agregar símbolo con valor
    public void agregarSimbolo(String nombre, String tipo, Integer valor, int linea, int columna) {
        if (!existeSimbolo(nombre)) {
            tablaSimbolos.add(new EntradaTablaSimbolos(nombre, tipo, valor, linea, columna));
        }
    }
    
    // Verificar si un símbolo ya existe
    public boolean existeSimbolo(String nombre) {
        return tablaSimbolos.stream()
                .anyMatch(entrada -> entrada.getNombre().equals(nombre));
    }
    
    // Buscar un símbolo específico
    public EntradaTablaSimbolos buscarSimbolo(String nombre) {
        return tablaSimbolos.stream()
                .filter(entrada -> entrada.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }
    
    // Actualizar valor de un símbolo existente
    public boolean actualizarValor(String nombre, Integer nuevoValor) {
        EntradaTablaSimbolos entrada = buscarSimbolo(nombre);
        if (entrada != null) {
            entrada.setValorAsignado(nuevoValor);
            return true;
        }
        return false;
    }
    
    // Obtener lista completa
    public List<EntradaTablaSimbolos> getTablaSimbolos() {
        return new ArrayList<>(tablaSimbolos);
    }
    
    // Filtrar por tipo
    public List<EntradaTablaSimbolos> getSimbolosPorTipo(String tipo) {
        return tablaSimbolos.stream()
                .filter(entrada -> entrada.getTipo().equals(tipo))
                .collect(Collectors.toList());
    }
    
    // Limpiar tabla
    public void limpiarTabla() {
        tablaSimbolos.clear();
    }
    
    public void imprimirTablaSimbolosEnTabla() {
        System.out.println("\n--- Tabla de Símbolos ---");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-15s %-10s %-12s %-8s %-8s\n", "VARIABLE", "TIPO", "VALOR", "LINEA", "COLUMNA");
        System.out.println("------------------------------------------------------------------");
        
        for (EntradaTablaSimbolos entrada : tablaSimbolos) {
            String variable = (entrada.getNombre() != null) ? entrada.getNombre() : "";
            String tipo = (entrada.getTipo() != null) ? entrada.getTipo() : "";
            String valor = (entrada.getValorAsignado() != null) ? entrada.getValorAsignado().toString() : "no asignado";
            String linea = String.valueOf(entrada.getLineaDeclaracion());
            String columna = String.valueOf(entrada.getColumnaDeclaracion());
            
            System.out.printf("%-15s %-10s %-12s %-8s %-8s\n", 
                            variable, 
                            tipo, 
                            valor, 
                            linea, 
                            columna);
        }
        System.out.println("------------------------------------------------------------------");
    }
    
    public String obtenerTablaSimbolosEnFormatoTabla() {
        StringBuilder tabla = new StringBuilder();
        tabla.append("\n--- Tabla de Símbolos ---\n");
        tabla.append("------------------------------------------------------------------\n");
        tabla.append(String.format("%-15s %-10s %-12s %-8s %-8s\n", "VARIABLE", "TIPO", "VALOR", "LINEA", "COLUMNA"));
        tabla.append("------------------------------------------------------------------\n");
        
        for (EntradaTablaSimbolos entrada : tablaSimbolos) {
            String variable = (entrada.getNombre() != null) ? entrada.getNombre() : "";
            String tipo = (entrada.getTipo() != null) ? entrada.getTipo() : "";
            String valor = (entrada.getValorAsignado() != null) ? entrada.getValorAsignado().toString() : "no asignado";
            String linea = String.valueOf(entrada.getLineaDeclaracion());
            String columna = String.valueOf(entrada.getColumnaDeclaracion());
            
            tabla.append(String.format("%-15s %-10s %-12s %-8s %-8s\n", 
                                     variable, tipo, valor, linea, columna));
        }
        tabla.append("------------------------------------------------------------------\n");
        return tabla.toString();
    }
    
    // Método para cargar datos desde la tabla existente del Analizador
    public void cargarDesdeAnalizador() {
        List<Analizador.EntradaTablaSimbolos> tablaCompleta = Analizador.getTablaSimbolosCompleta();
        limpiarTabla(); // Limpiar primero
        
        for (Analizador.EntradaTablaSimbolos entrada : tablaCompleta) {
            agregarSimbolo(
                entrada.getNombre(), 
                entrada.getTipo(), 
                entrada.getValorAsignado(),
                entrada.getLineaDeclaracion(), 
                entrada.getColumnaDeclaracion()
            );
        }
    }
    
    // Método para obtener el tamaño de la tabla
    public int getTamaño() {
        return tablaSimbolos.size();
    }
    
    // Método para verificar si la tabla está vacía
    public boolean estaVacia() {
        return tablaSimbolos.isEmpty();
    }
    
    // Método para mostrar tabla en frame (NUEVO - similar a tu estructura)
    public void mostrarTablaEnFrame() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            VentanaTablaSimbolos ventana = new VentanaTablaSimbolos(this);
            ventana.setVisible(true);
        });
    }
    
    public static class EntradaTablaSimbolos {
        private String nombre;
        private String tipo;
        private int lineaDeclaracion;
        private int columnaDeclaracion;
        private Integer valorAsignado;

        public EntradaTablaSimbolos(String nombre, String tipo, int lineaDeclaracion, int columnaDeclaracion) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.valorAsignado = null;
            this.lineaDeclaracion = lineaDeclaracion;
            this.columnaDeclaracion = columnaDeclaracion;
        }
        
        public EntradaTablaSimbolos(String nombre, String tipo, Integer valorAsignado, int lineaDeclaracion, int columnaDeclaracion) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.valorAsignado = valorAsignado;
            this.lineaDeclaracion = lineaDeclaracion;
            this.columnaDeclaracion = columnaDeclaracion;
        }

        // Getters y setters
        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
        public Integer getValorAsignado() { return valorAsignado; }
        public void setValorAsignado(Integer valorAsignado) { this.valorAsignado = valorAsignado; }
        public int getLineaDeclaracion() { return lineaDeclaracion; }
        public int getColumnaDeclaracion() { return columnaDeclaracion; }

        @Override
        public String toString() {
            return String.format("%-15s | %-10s | L%-2d:C%-2d",
                    nombre, tipo, lineaDeclaracion, columnaDeclaracion);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            EntradaTablaSimbolos that = (EntradaTablaSimbolos) obj;
            return nombre.equals(that.nombre);
        }

        @Override
        public int hashCode() {
            return nombre.hashCode();
        }
    }
}