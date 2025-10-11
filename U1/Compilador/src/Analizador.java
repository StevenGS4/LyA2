
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Analizador {

    public static class Token {

        String tipo;
        String lexema;
        int linea;
        int columna;

        public Token(String tipo, String lexema, int linea, int columna) {
            this.tipo = tipo;
            this.lexema = lexema;
            this.linea = linea;
            this.columna = columna;
        }

        @Override
        public String toString() {
            return tipo + ": " + lexema + " (línea " + linea + ", col " + columna + ")";
        }
    }

    public Analizador() {
    }
    
    

    // PATRONES DE TOKENIZACIÓN
    private static final String[][] patrones = {
                                                                    
        {"PALABRA_CLAVE", "\\b(fin si|fin para|fin mientras|fin caso)\\b"},
        
                                                                                                                    //se agregaron los metodos
        {"PALABRA_CLAVE", "\\b(iniciar|Terminar|si|entonces|detener|para|mientras|caso|hacer|hasta|cuando|romper|fin|tipo|iniciar_metodo|fin_metodo)\\b"},
        {"COMANDO_MOVIMIENTO", "\\b(mover_adelante|mover_atras|girar_izquierda|girar_derecha)\\b"},
        {"COMANDO_ACTUADOR", "\\b(encender_led|apagar_led)\\b"},
        {"COMANDO_SENSOR", "\\b(leer_sensor)\\b"},
        {"COMANDO_TIEMPO", "\\b(esperar)\\b"},
        {"NUMERO", "\\b\\d+(\\.\\d+)?\\b"},
        {"OPERADOR_ARITMETICO", "(\\+|\\-|\\*|\\/)"},
        {"OPERADOR_REL", "(<=|>=|==|!=|<|>)"},
        {"OPERADOR_LOGICO", "(\\&\\&|\\|\\||!)"},
        {"OPERADOR_ASIGNACION", "="},
        {"UNIDAD_TIEMPO", "\\b(segundos|milisegundos)\\b"},
        {"TIPOS_DATOS", "\\b(numero|Sensor)\\b"},
        {"PARENTESIS", "(\\(|\\))"},
        {"IDENTIFICADOR", "\\b[a-zA-Z_][a-zA-Z_0-9]*\\b"},
        {"COMENTARIO", "(//.*|#.*)"},
        {"SEPARADOR",","}/////se agregó el separador
    };

    
    //lista de la tabla de simbolos completa
    private static final List<EntradaTablaSimbolos> tablaSimbolosCompleta = new ArrayList<>();
    
    //solo son los patrones que el analziador léxico reconoció 
    private static final List<Object[]> patronesCompilados = new ArrayList<>();

    static {
        for (String[] patron : patrones) {
            patronesCompilados.add(new Object[]{patron[0], Pattern.compile("^" + patron[1])});
        }
    }

    /**
     * Convierte código fuente en una lista de tokens
     */
    public static List<Token> analizarLexico(String codigo) {
        // 
        List<Token> tokens = new ArrayList<>();
        String[] lineas = codigo.split("\\R");

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            int columna = 1;

            while (!linea.isEmpty()) {
                linea = linea.stripLeading();
                if (linea.isEmpty()) {
                    break;
                }

                boolean encontrado = false;

                for (Object[] patron : patronesCompilados) {
                    String tipo = (String) patron[0];
                    Pattern regex = (Pattern) patron[1];
                    Matcher matcher = regex.matcher(linea);

                    if (matcher.find()) {
                        String lexema = matcher.group();
                        tokens.add(new Token(tipo, lexema, i + 1, columna));
                        columna += lexema.length();
                        linea = linea.substring(lexema.length());
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) {
                    int fin = linea.indexOf(" ");
                    String errorLexema = (fin == -1) ? linea : linea.substring(0, fin);
                    tokens.add(new Token("ERROR LEXICO", errorLexema, i + 1, columna));

                    columna += errorLexema.length();
                    linea = linea.substring(errorLexema.length());
                }
            }
        }

        //Construir tabla de símbolos después de tokenizar
        construirTablaSimbolos(tokens);

        return tokens;
    }

    
    
    
    /**
     * Construye la tabla de símbolos analizando tokens para encontrar
     * declaraciones
     */
    private static void construirTablaSimbolos(List<Token> tokens){
        // Limpiar tabla completa
        tablaSimbolosCompleta.clear();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Detectar patrón de declaración: IDENTIFICADOR "tipo" TIPO "=" valor
            if (esPatronDeclaracion(tokens, i)) {
                String nombreVariable = token.lexema;
                String tipoVariable = tokens.get(i + 2).lexema;
                int lineaDeclaracion = token.linea;
                int columnaDeclaracion = token.columna;

                // Verificar que es un tipo válido
                if (esTipoValido(tipoVariable)) {
                    // Evitar duplicados en tabla completa
                    if (!existeVariableEnTabla(nombreVariable)) {
                        EntradaTablaSimbolos nuevaEntrada = new EntradaTablaSimbolos(
                                nombreVariable, tipoVariable, lineaDeclaracion, columnaDeclaracion);
                        tablaSimbolosCompleta.add(nuevaEntrada);
                    }
                }
            }
        }
    }
    
     public static String obtenerPatronesPorLinea(List<Token> tokens) {
        StringBuilder resultado = new StringBuilder();
        int lineaActual = -1;
        
        for (Token token : tokens) {
            if (token.linea != lineaActual) {
                lineaActual = token.linea;
                if (resultado.length() > 0) {
                    resultado.append("\n");
                }
                resultado.append("[Línea ").append(lineaActual).append("]\n");
            }

            String patronUsado = null;
            for (String[] patron : patrones) {
                if (token.tipo.equals(patron[0])) {
                    patronUsado = patron[1];
                    break;
                }
            }

            if (patronUsado != null) {
                resultado.append(String.format("%-18s: %-15s ➝ %s\n", 
                    token.tipo, token.lexema, patronUsado));
            } else {
                resultado.append(String.format("%-18s: %-15s ➝ [no patrón]\n", 
                    token.tipo, token.lexema));
            }
        }
        return resultado.toString();
    }
    

    /**
     * Verifica si la posición actual forma un patrón de declaración válido
     */
    private static boolean esPatronDeclaracion(List<Token> tokens, int posicion) {
        
       
        
        if (posicion + 4 >= tokens.size()) {
            return false;
        }

        Token token = tokens.get(posicion);
        return token.tipo.equals("IDENTIFICADOR")
                && tokens.get(posicion + 1).lexema.equals("tipo")
                && tokens.get(posicion + 3).lexema.equals("=");
    }
    
    
    public boolean esDeMetodo(){
        
        return false;
    }
    

    /**
     * Verifica si es un tipo de datos válido
     */
    private static boolean esTipoValido(String tipo) {
        return tipo.equals("numero") || tipo.equals("Sensor");
    }

    /**
     * Verifica si una variable ya existe en la tabla completa
     */
    private static boolean existeVariableEnTabla(String nombreVariable) {
        return tablaSimbolosCompleta.stream()
                .anyMatch(entrada -> entrada.getNombre().equals(nombreVariable));
    }

    /**
     * Obtiene la tabla de símbolos completa
     */
    public static List<EntradaTablaSimbolos> getTablaSimbolosCompleta() {
        return new ArrayList<>(tablaSimbolosCompleta);
    }

    /**
     * Busca una variable específica en la tabla
     */
    public static EntradaTablaSimbolos buscarVariable(String nombre) {
        return tablaSimbolosCompleta.stream()
                .filter(entrada -> entrada.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene la tabla en formato texto 
     */
    public static String getTablaSimbolosTexto() {
        if (tablaSimbolosCompleta.isEmpty()) {
            return "═══ TABLA DE SÍMBOLOS ═══\n"
                    + "No se encontraron declaraciones de variables.\n"
                    + "═══════════════════════════";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ TABLA DE SÍMBOLOS ═══\n");
        sb.append(String.format("%-15s | %-10s | %s\n",
                "Variable", "Tipo", "Posición"));
        sb.append("─".repeat(45)).append("\n");

        for (EntradaTablaSimbolos entrada : tablaSimbolosCompleta) {
            sb.append(entrada.toString()).append("\n");
        }

        sb.append("─".repeat(45)).append("\n");
        sb.append("Total: ").append(tablaSimbolosCompleta.size()).append(" variables declaradas");

        return sb.toString();
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

        public String getNombre() {
            return nombre;
        }

        public String getTipo() {
            return tipo;
        }
        
        public Integer getValorAsignado() {
            return valorAsignado;
        }

        public void setValorAsignado(Integer valorAsignado) {
            this.valorAsignado = valorAsignado;
        }

        public int getLineaDeclaracion() {
            return lineaDeclaracion;
        }

        public int getColumnaDeclaracion() {
            return columnaDeclaracion;
        }

        @Override
        public String toString() {
            return String.format("%-15s | %-10s | L%-2d:C%-2d",
                    nombre, tipo, lineaDeclaracion, columnaDeclaracion);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            EntradaTablaSimbolos that = (EntradaTablaSimbolos) obj;
            return nombre.equals(that.nombre);
        }

        @Override
        public int hashCode() {
            return nombre.hashCode();
        }
    }
    
    
    //Se cambió este método para poder construir el árbol sintáctico
    public static boolean analizarSintaxis(List<Token> tokens, StringBuilder errores) {
        //usamos el AST
        AnalizadorSintactico parser = new AnalizadorSintactico();
        ProgramaNodo ast = parser.parsear(tokens, getTablaSimbolosCompleta()); //se modificpo para que el parser tenga la tabla de simbolos
        
        if (ast == null) {
            errores.append(parser.getErrores());
            return false;
        }
        return true;
    }
    
    public static ProgramaNodo parsearAST(String codigo, StringBuilder errores) {
        // 1. Análisis léxico
        List<Token> tokens = analizarLexico(codigo);
        
        // 2. Verificar errores léxicos
        for (Token token : tokens) {
            if ("ERROR LEXICO".equals(token.tipo)) {
                errores.append("Error léxico: '").append(token.lexema)
                       .append("' en línea ").append(token.linea).append("\n");
                return null;
            }
        }
        
        // 3. AnalizadorSintactico, parseo en cascada
        AnalizadorSintactico parser = new AnalizadorSintactico();
        ProgramaNodo ast = parser.parsear(tokens, getTablaSimbolosCompleta());//SE LE PASA AL PARSER LA TABLA DE SIMBOLOS COMPLETA
        
        if (ast == null) {
            errores.append(parser.getErrores());
            return null;
        }
        
        return ast;
    }
    

private static GestorTablaSimbolos gestorTablaSimbolos = new GestorTablaSimbolos();

public static GestorTablaSimbolos getGestorTablaSimbolos() {
    return gestorTablaSimbolos;
}

// Método para sincronizar la tabla actual con el gestor
public static void sincronizarConGestor() {
    gestorTablaSimbolos.limpiarTabla();
    for (EntradaTablaSimbolos entrada : tablaSimbolosCompleta) {
        gestorTablaSimbolos.agregarSimbolo(
            entrada.getNombre(),
            entrada.getTipo(),
            entrada.getValorAsignado(),
            entrada.getLineaDeclaracion(),
            entrada.getColumnaDeclaracion()
        );
    }
}

private static void construirTablaSimbolosBuena(List<Token> tokens) {
    // Limpiar tabla completa
    tablaSimbolosCompleta.clear();
    gestorTablaSimbolos.limpiarTabla();

    for (int i = 0; i < tokens.size(); i++) {
        Token token = tokens.get(i);

        // Detectar patrón de declaración: IDENTIFICADOR "tipo" TIPO "=" valor
        if (esPatronDeclaracion(tokens, i)) {
            String nombreVariable = token.lexema;
            String tipoVariable = tokens.get(i + 2).lexema;
            int lineaDeclaracion = token.linea;
            int columnaDeclaracion = token.columna;

            // Verificar que es un tipo válido
            if (esTipoValido(tipoVariable)) {
                // Evitar duplicados en tabla completa
                if (!existeVariableEnTabla(nombreVariable)) {
                    EntradaTablaSimbolos nuevaEntrada = new EntradaTablaSimbolos(
                            nombreVariable, tipoVariable, lineaDeclaracion, columnaDeclaracion);
                    tablaSimbolosCompleta.add(nuevaEntrada);
                    
                    // También agregar al gestor
                    gestorTablaSimbolos.agregarSimbolo(
                        nombreVariable, tipoVariable, lineaDeclaracion, columnaDeclaracion);
                }
            }
        }
    }
}

public static String getTablaSimbolosTextoConGestor() {
    sincronizarConGestor(); // Asegurar sincronización
    return gestorTablaSimbolos.obtenerTablaSimbolosEnFormatoTabla();
}

// Método para imprimir tabla usando el gestor
public static void imprimirTablaSimbolosConGestor() {
    sincronizarConGestor();
    gestorTablaSimbolos.imprimirTablaSimbolosEnTabla();
}



// Método para mostrar tabla en frame usando el gestor
public static void mostrarTablaSimbolosEnFrame() {
    sincronizarConGestor();
    gestorTablaSimbolos.mostrarTablaEnFrame();
}


   }
