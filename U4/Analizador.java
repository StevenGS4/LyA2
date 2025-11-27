
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Analizador {

    
 /**
 * Representa un token reconocido por el analizador léxico.
 * Contiene:
 *  - tipo: nombre del patrón (ej. IDENTIFICADOR, NUMERO, PALABRA_CLAVE)
 *  - lexema: la cadena exacta del token en el código fuente
 *  - linea, columna: posición donde se encontró el token (1-indexed)
 */

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

    
    
 /**
 * Constructor por defecto. La clase utiliza mayormente miembros estáticos,
 * por lo que no es necesario instanciarla para usar las utilidades estáticas.
 */
    
    public Analizador() {
    }
    
    
/**
 * Matriz de patrones léxicos: {tipo, regex}. 
 * Orden importante: los patrones se prueban en el orden en que aparecen.
 */

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

    
/**
 * Lista de pares {tipo, Pattern} construida a partir de `patrones`.
 * Se compilan aquí para reutilizarlas durante el análisis léxico.
 */    
    
    static {
        for (String[] patron : patrones) {
            patronesCompilados.add(new Object[]{patron[0], Pattern.compile("^" + patron[1])});
        }
    }

    /**
     * Convierte código fuente en una lista de tokens
     */
    
/**
 * Realiza el análisis léxico (tokenización) del código fuente.
 *
 * @param codigo El código fuente completo como un único String (puede contener varias líneas).
 * @return Lista de Token detectados en el código. Si se encuentra texto que no coincide
 *         con ningún patrón, se genera un Token con tipo "ERROR LEXICO".
 *
 * Efectos secundarios: Llama a construirTablaSimbolos(...) para poblar la tabla de símbolos.
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
    
      
/**
 * Analiza la lista de tokens y construye la tabla de símbolos completa.
 *
 * Busca declaraciones que siguen el patrón:
 *    IDENTIFICADOR 'tipo' TIPO '=' ...
 * y agrega una entrada por cada declaración válida (sin duplicados).
 *
 * @param tokens Lista de tokens generada por el analizador léxico.
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
    
    
    
/**
 * Construye una representación textual que muestra, por cada línea, los tokens detectados
 * y el patrón (regex) original que los generó.
 *
 * @param tokens Lista de tokens generada por el analizador léxico.
 * @return Un String formateado con tokens agrupados por línea y el patrón asociado.
 */
    
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
     
/**
 * Determina si en la lista de tokens, desde `posicion`, comienza una declaración válida.
 *
 * Patrón reconocido: IDENTIFICADOR 'tipo' TIPO '='
 *
 * @param tokens Lista de tokens.
 * @param posicion Índice en la lista donde se inicia la comprobación.
 * @return true si los tokens desde `posicion` forman una declaración válida.
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
    
 /**
 * Método placeholder: determinación si el contexto actual pertenece a un método para no marcar errores de sintaxis.
 * Actualmente implementado y siempre retorna false.
 *
 * @return false
 */

    public boolean esDeMetodo(){
        
        return false;
    }
    

    /**
     * Verifica si es un tipo de datos válido
     */
    
/**
 * Verifica si `tipo` es un tipo de dato válido soportado por el analizador.
 *
 * @param tipo Nombre del tipo (ej. "numero", "Sensor")
 * @return true si el tipo es válido, false en caso contrario.
 */
    
    private static boolean esTipoValido(String tipo) {
        return tipo.equals("numero") || tipo.equals("Sensor");
    }

    /**
     * Verifica si una variable ya existe en la tabla completa
     */
    
/**
 * Comprueba si ya existe una entrada en la tabla de símbolos completa con el nombre dado.
 *
 * @param nombreVariable Nombre de la variable a buscar.
 * @return true si existe, false si no.
 */
    
    private static boolean existeVariableEnTabla(String nombreVariable) {
        return tablaSimbolosCompleta.stream()
                .anyMatch(entrada -> entrada.getNombre().equals(nombreVariable));
    }

    /**
     * Obtiene la tabla de símbolos completa
     */
    
/**
 * Devuelve una copia de la tabla de símbolos completa.
 *
 * @return Lista con las entradas de la tabla de símbolos.
 */
    
    public static List<EntradaTablaSimbolos> getTablaSimbolosCompleta() {
        return new ArrayList<>(tablaSimbolosCompleta);
    }

    /**
     * Busca una variable específica en la tabla
     */
    
/**
 * Busca una entrada de tabla de símbolos por nombre.
 *
 * @param nombre Nombre de la variable a buscar.
 * @return EntradaTablaSimbolos si existe, o null si no se encuentra.
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
    
/**
 * Genera una representación textual (tabla) de la tabla de símbolos completa.
 *
 * @return String con la tabla formateada lista para mostrarse en consola/UI.
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

    
/**
 * Representa una entrada (símbolo) en la tabla de símbolos.
 * Contiene nombre, tipo, posición de declaración y valor asignado opcional.
 */
    
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
    

/**
 * Inicia el análisis sintáctico a partir de una lista de tokens.
 *
 * @param tokens Lista de tokens generada por el analizador léxico.
 * @param errores StringBuilder donde se anexarán mensajes de error del parser.
 * @return true si el análisis sintáctico fue exitoso (AST no nulo), false si hubo errores.
 *
 * Nota: el parser recibe la tabla de símbolos completa para poder validar declaraciones/escopes.
 */
    
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
    
    
    
/**
 * Flujo completo para obtener el AST (ProgramaNodo) a partir del código fuente.
 *
 * 1. Realiza análisis léxico.
 * 2. Si hay errores léxicos, los reporta y retorna null.
 * 3. Ejecuta el analizador sintáctico (parser) pasándole la tabla de símbolos.
 *
 * @param codigo Código fuente completo.
 * @param errores StringBuilder donde se anexarán mensajes de error (léxicos y sintácticos).
 * @return ProgramaNodo (AST) si el parseo fue exitoso, o null si hubo errores.
 */

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
    
/** Devuelve la instancia del GestorTablaSimbolos usada por Analizador. */
private static GestorTablaSimbolos gestorTablaSimbolos = new GestorTablaSimbolos();

public static GestorTablaSimbolos getGestorTablaSimbolos() {
    return gestorTablaSimbolos;
}

/**
 * Sincroniza la tabla de símbolos interna con el GestorTablaSimbolos.
 * Limpia primero el gestor y luego agrega todas las entradas actuales.
 */

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


/**
 * Variante de construcción de la tabla de símbolos que además sincroniza las entradas
 * en el GestorTablaSimbolos durante la creación.
 */

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


/**
 * Operaciones que muestran/obtienen la tabla de símbolos usando el GestorTablaSimbolos.
 * Se sincroniza primero para asegurar que el gestor contiene las entradas actuales.
 */

public static String getTablaSimbolosTextoConGestor() {
    sincronizarConGestor(); // Asegurar sincronización
    return gestorTablaSimbolos.obtenerTablaSimbolosEnFormatoTabla();
}

// Método para imprimir tabla usando el gestor
/**
 * Operaciones que muestran/obtienen la tabla de símbolos usando el GestorTablaSimbolos.
 * Se sincroniza primero para asegurar que el gestor contiene las entradas actuales.
 */

public static void imprimirTablaSimbolosConGestor() {
    sincronizarConGestor();
    gestorTablaSimbolos.imprimirTablaSimbolosEnTabla();
}

/**
 * Operaciones que muestran/obtienen la tabla de símbolos usando el GestorTablaSimbolos.
 * Se sincroniza primero para asegurar que el gestor contiene las entradas actuales.
 */

// Método para mostrar tabla en frame usando el gestor
public static void mostrarTablaSimbolosEnFrame() {
    sincronizarConGestor();
    gestorTablaSimbolos.mostrarTablaEnFrame();
}


   }
