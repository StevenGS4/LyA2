import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;



/**
 * Analizador Sintáctico que construye un AST (Árbol Sintáctico Abstracto)
 * Implementa un Parser Descendente Recursivo siguiendo la gramática definida
 */
public class AnalizadorSintactico {
    
    // Estado del parser
    private List<Analizador.Token> tokens;
    private int posicion;
    private Set<Integer> pinesSensorUsados; 
   
    private StringBuilder errores;
    
    //obtendrá la tabla de simbolos del analizador. 
    private List<Analizador.EntradaTablaSimbolos> tablaSimbolos;
    
    //esto creará una lista temporal de las variables declaradas, para usar la tabla de simbolos del analizador léxico
        private Set<String> variablesDeclaradasEnParsing;
        private Set<String> metodos;//para checar que los identificadores de métodos no se repitan
            private GeneradorCodigoIntermedio generadorCodigo;


    public AnalizadorSintactico(List<Analizador.Token> tokens, List<Analizador.EntradaTablaSimbolos> tablaSimbolos) {
        this.tokens = tokens;
        this.posicion = 0;
        this.errores = new StringBuilder();
        this.tablaSimbolos = tablaSimbolos;
        this.variablesDeclaradasEnParsing = new HashSet<>();
        this.metodos = new HashSet<>();
        this.generadorCodigo = new GeneradorCodigoIntermedio();
        this.generadorCodigo = new GeneradorCodigoIntermedio(); 
    }

    // *** AÑADIR ESTE MÉTODO GETTER ***
    public GeneradorCodigoIntermedio getGeneradorCodigo() {
        return generadorCodigo;
    }
    
        private void validarPinSensorNoDuplicado(int pin, int linea, String identificadorSensor) throws ParseException {
        if (pinesSensorUsados.contains(pin)) {
            String sensorOriginal = tablaSimbolos.stream()
                .filter(entrada -> "Sensor".equals(entrada.getTipo()) && entrada.getValorAsignado() != null && entrada.getValorAsignado().equals(pin))
                .map(Analizador.EntradaTablaSimbolos::getNombre)
                .findFirst()
                .orElse("otro sensor"); 

            throw new ParseException("Error: El pin '" + pin + "' ya está siendo utilizado por el sensor '" + sensorOriginal + "'. No se puede asignar a '" + identificadorSensor + "' en línea " + linea);
        }
        pinesSensorUsados.add(pin);
    }
    
    //Esto verifica que una variable sí esté declarada en el programa antes de su uso dentro de cualquier parte del programa
   private void validarVariableDeclarada(String nombreVariable, int linea) throws ParseException {
    boolean existe = tablaSimbolos.stream()
        .anyMatch(entrada -> entrada.getNombre().equals(nombreVariable));
    
    if (!existe) {
        throw new ParseException("Variable '" + nombreVariable + 
                               "' no fue declarada antes de su uso en línea " + linea);
    }
}
   //Para evitar duplicidad en las declaraciones, checando las variables temporales. 

private void validarNoDuplicada(String nombreVariable, int linea) throws ParseException {
    if (variablesDeclaradasEnParsing.contains(nombreVariable)) {
        throw new ParseException("Variable '" + nombreVariable + 
                               "' ya fue declarada previamente en línea " + linea);
    }
    // Agregar la variable al conjunto de declaradas
    variablesDeclaradasEnParsing.add(nombreVariable);
}






    public AnalizadorSintactico() {
        this.posicion = 0;
        this.errores = new StringBuilder();
                this.pinesSensorUsados = new HashSet<>(); // Inicializar aquí
    }
    
    
     // Parsea una lista de tokens y construye el AST
      //tokens Lista de tokens del análisis léxico
     //retorna ProgramaNodo raíz del AST, o null si hay errores
     
    
    
    //se agregó la segunda lista para poder poner la tabla de simbolos
    public ProgramaNodo parsear(List<Analizador.Token> tokens, 
                                List<Analizador.EntradaTablaSimbolos> tablaSimbolos) {
        this.tokens = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.posicion = 0;
        this.errores.setLength(0);
        this.pinesSensorUsados = new HashSet<>(); // Re-inicializar para cada operación de parseo
        this.variablesDeclaradasEnParsing = new HashSet<>();
        this.metodos = new HashSet<>();
        
        ProgramaNodo programaAST = null; // Declara la variable aquí
        
          // Asegurar que el generador está inicializado
        if (this.generadorCodigo == null) {
            this.generadorCodigo = new GeneradorCodigoIntermedio();
        }

        try {
           
            programaAST = parsearPrograma(); 

         
            if (programaAST != null && errores.length() == 0) { 
                programaAST.generarCodigoIntermedio(this.generadorCodigo); 
               
            }
            return programaAST; // Retorna el AST
        } catch (ParseException e) {
            errores.append(e.getMessage()).append("\n");
            return null; // Retorna null si hay un error de parseo
        }
    }
    
    
    
    /**
     * Obtiene los errores de parsing
     */
    public String getErrores() {
        return errores.toString();
    }
    
    
    
    /**
     * Obtiene el token actual sin consumirlo
     */
    private Analizador.Token tokenActual() {
        if (posicion >= tokens.size()) {
            return null;
        }
        return tokens.get(posicion);
    }
    
    /**
     * Verifica si hay más tokens
     */
    private boolean hayTokens() {
        return posicion < tokens.size();
    }
    
     ///////////////////////////////////con 0/////////////////////////////////

    private void validarDivisionesPorCero(ExpresionNodo expresion) throws ParseException {
        if (expresion instanceof ExpresionAritmeticaNodo) {
            ExpresionAritmeticaNodo exp = (ExpresionAritmeticaNodo) expresion;

            //revisar operando derecho
            for (int i = 0; i < exp.getOperadores().size(); i++) {
                if (exp.getOperadores().get(i).equals("/")) {
                    ExpresionNodo operandoDerecho = exp.getOperandos().get(i + 1);
                    if (esCero(operandoDerecho)) {
                        throw new ParseException("Division entre cero detectada en linea " + expresion.getLinea());
                    }
                }
            }

            for (ExpresionNodo operando : exp.getOperandos()) {
                validarDivisionesPorCero(operando);
            }
        }
    }


    private boolean esCero(ExpresionNodo nodo) {
        if (nodo instanceof NumeroNodo) {
            return ((NumeroNodo) nodo).getValor() == 0;
        }
        return false;
    }
    
    
    //recibe el lexema esperado y retorna el token consumido
    private Analizador.Token consumir(String lexemaEsperado) throws ParseException {
        Analizador.Token token = tokenActual();
        
        if (token == null) {
            throw new ParseException("Se esperaba '" + lexemaEsperado + "' pero se alcanzó el final del archivo");
        }
        
        if (!token.lexema.equals(lexemaEsperado)) {
            throw new ParseException("Se esperaba '" + lexemaEsperado + "' pero se encontró '" + 
                                   token.lexema + "' en línea " + token.linea);
        }
        
        posicion++;
        return token;
    }
    
    
    //similar al anterior, pero en lugar de un lexema, trabaja con Tipo de token esperado (identificador, numero, los tokens lexicos, etc)
    private Analizador.Token consumirTipo(String tipoEsperado) throws ParseException {
        Analizador.Token token = tokenActual();
        
        if (token == null) {
            throw new ParseException("Se esperaba " + tipoEsperado + " pero se alcanzó el final del archivo");
        }
        
        if (!token.tipo.equals(tipoEsperado)) {
            throw new ParseException("Se esperaba " + tipoEsperado + " pero se encontró " + 
                                   token.tipo + " ('" + token.lexema + "') en línea " + token.linea);
        }
        
        posicion++;
        return token;
    }
    
    /**
     * Verifica si el token actual coincide con el lexema esperado
     */
    private boolean verificar(String lexema) {
        Analizador.Token token = tokenActual();
        return token != null && token.lexema.equals(lexema);
    }
    
    /**
     * Verifica si el token actual es del tipo esperado
     */
    private boolean verificarTipo(String tipo) {
        Analizador.Token token = tokenActual();
        return token != null && token.tipo.equals(tipo);
    }
    
    /**
     * Verifica si el token actual es un operador aritmético (+, -, *, /)
     */
    private boolean verificarOperadorAritmetico(String operador) {
        Analizador.Token token = tokenActual();
        return token != null && 
               (token.tipo.equals("OPERADOR_ARITMETICO") || token.lexema.equals(operador));
    }
    
    /**
     * Verifica si el token actual es paréntesis
     */
    private boolean verificarParentesis(String parentesis) {
        Analizador.Token token = tokenActual();
        return token != null && 
               (token.tipo.equals("PARENTESIS") || token.lexema.equals(parentesis));
    }
    
    /**
     * Salta comentarios
     */
    private void saltarComentarios() {
        while (hayTokens() && verificarTipo("COMENTARIO")) {
            posicion++;
        }
    }
    
    
    
    //métodos del parseo, es uno por cada regla gramatical
    
    /**
     * <programa> ::= "iniciar" <declaraciones> <instrucciones> "Terminar"
     */
    private ProgramaNodo parsearPrograma() throws ParseException {
        saltarComentarios();
        
        // Consumir "iniciar"
        Analizador.Token iniciar = consumir("iniciar");
        ProgramaNodo programa = new ProgramaNodo(iniciar.linea, iniciar.columna);
        
        saltarComentarios();
        
        // Parsear declaraciones
        parsearDeclaraciones(programa);
        
        saltarComentarios();
        
        parsearDeclaracionesMetodos(programa);
        
        // Parsear instrucciones
        parsearInstrucciones(programa);
        
        saltarComentarios();
        
        // Consumir "Terminar"
        consumir("Terminar");
        
        return programa;
    }
    
    /**
     * <declaraciones> ::= <declaracion> <declaraciones> | ε
     */
    private void parsearDeclaraciones(ProgramaNodo programa) throws ParseException {
        saltarComentarios();
        
        // Mientras haya declaraciones (identificador seguido de "tipo")
        while (hayTokens() && verificarTipo("IDENTIFICADOR")) {
            // Mirar hacia adelante para ver si es declaración
            if (posicion + 1 < tokens.size() && tokens.get(posicion + 1).lexema.equals("tipo")) {
                DeclaracionNodo declaracion = parsearDeclaracion();
                programa.agregarDeclaracion(declaracion);
                saltarComentarios();
            } else {
                break; // No es declaración, salir del bucle
            }
        }
    }
    
    
    //==========================método para parsear declaracionmetodo en un ciclo, hasta acabar
    
     /**
     * para:     <identificador> "("  <parametros>  ")"<declaraciones> <instrucciones>
     */
    private DeclaracionMetodoNodo parsearMetodo() throws ParseException{
       //esta ubicado en un id
        Analizador.Token token = tokenActual();
       DeclaracionMetodoNodo metodos = new DeclaracionMetodoNodo(token.lexema, posicion, posicion);
        consumirTipo("IDENTIFICADOR");
        consumir("(");
        
        token =tokenActual();
        
        //para metodos sin parametros FUNCIONA
        if (token.lexema.equals(")")) {
            consumir(")");
            parsearDeclaraciones(metodos);
            
            parsearInstrucciones(metodos);
                       consumir("fin_metodo");

            return metodos;
           
        }
        

        //id tipo numero|Sensor, uno o mas parametros

        while(hayTokens() && verificarTipo("IDENTIFICADOR")){
            //significa que hay un id y falta "tipo nummero|Sensor"
            
            
            ParametroNodo parametro = parsearParametroNodo();
                if(!metodos.agregarParametro(parametro))
                    throw new ParseException("Error en línea: "+parametro.linea+
                ". El parámetro con identificador: "+parametro.getIdentificador()+" ya fue declarado.");
                
                saltarComentarios();
                
                //checar si hay coma
                if (tokenActual().lexema.equals(",")) {
                    //se vuelve a parseat el parametro
                    consumir(",");
                    if (!verificarTipo("IDENTIFICADOR")) {
                       throw new ParseException("Se esperaba identificador después de ',' en línea " + tokenActual().linea);
        }
            }
        //una vez agregado todo como sus parametros, declaraciones e instrucciones, devuelve el nodo con esas tres listas completas
        }    
        consumir(")");
            
            //se deben de parsear las declaraciones nuevas y las instrucciones;
            parsearDeclaraciones(metodos);
            
            parsearInstrucciones(metodos);
           consumir("fin_metodo");
         return metodos;
    }
    
    /**
     * 
     *<usar_metodo>::= <identificador> "(" (parametros | ε) ")"
     * @
     */
    
    private ParametroNodo parsearParametroNodo()throws ParseException {
        //estoy en un token identificador
        //la estructura es id tipo numero|Sensor;
        Analizador.Token parametro= tokenActual();
        String identificador = parametro.lexema;
        consumirTipo("IDENTIFICADOR");
        consumir("tipo");
        Analizador.Token token = tokenActual();//obtengo el token actual para compararlo con lo que espero
        System.out.println(token.lexema);
        
        if(token.lexema.equals("numero"))consumir ("numero");
        else if(token.lexema.equals("Sensor"))consumir ("Sensor");
        else throw new ParseException("Se esperaba 'numero' o 'Sensor' en la línea: "+token.linea);

        return new ParametroNodo(identificador, token.tipo, parametro.linea, parametro.columna);
    }
    
    //sobreescribir el método de parsear declaraciones para que acepte como parámetro un DeclaracionMetodoNodo
    private void parsearDeclaraciones(DeclaracionMetodoNodo metodos) throws ParseException {
        saltarComentarios();
        
        // Mientras haya declaraciones (identificador seguido de "tipo")
        while (hayTokens() && verificarTipo("IDENTIFICADOR")) {
            // Mirar hacia adelante para ver si es declaración
            if (posicion + 1 < tokens.size() && tokens.get(posicion + 1).lexema.equals("tipo")) {
                DeclaracionNodo declaracion = parsearDeclaracion();
                metodos.agregarDeclaracion(declaracion);
                saltarComentarios();
            } else {
                break; // No es declaración, salir del bucle
            }
        }
    }
    
    
    private void parsearDeclaracionesMetodos(ProgramaNodo programa) throws ParseException{
         saltarComentarios();
            while (hayTokens() && verificar("iniciar_metodo")) {
            if (posicion + 1 < tokens.size() && tokens.get(posicion + 1).tipo.equals("IDENTIFICADOR")) {
                consumir("iniciar_metodo");
               // Analizador.Token token = tokenActual();

                //Aquí se debería checar si el identificador de este método ya está en uso o no
                //===========================================
                
                //se debe de buscar el nombre del método dentro del hashset de metodos
                if(metodos.contains(tokenActual().lexema)){
                    throw new ParseException("Error en línea: "+tokenActual().linea+". Este método ya fue declarado previamente, ponerle otro nombre ");
                }
                
                metodos.add(tokenActual().lexema);
                
                
                //===========================================
                DeclaracionMetodoNodo metodo =parsearMetodo();
                programa.agregarMetodo(metodo);

                saltarComentarios();//esto es para checar si no estamos ante una palabra de cierre



            }else{
                throw new ParseException("Sintaxis de métodos: 'iniciar_metodo' <identificador>'('<parametros>')'<declaraciones><instrucciones>");
            }
        }

    }
    
    
    
    /**
     * <declaracion> ::= <declaracion_numero> | <declaracion_sensor>
     */
    private DeclaracionNodo parsearDeclaracion() throws ParseException {
        // <identificador> "tipo" ("numero" | "Sensor") "=" <valor>
        Analizador.Token identificador = consumirTipo("IDENTIFICADOR");
        consumir("tipo");
        
        Analizador.Token tipo = tokenActual();
        
        if (verificar("numero")) {
            return parsearDeclaracionNumero(identificador);
        } else if (verificar("Sensor")) {
            return parsearDeclaracionSensor(identificador);
        } else {
            throw new ParseException("Se esperaba 'numero' o 'Sensor' después de 'tipo' en línea " + 
                                   identificador.linea);
        }
    }
    
    /**
     * <declaracion_metodo> ::= "iniciar_metodo" <identificador> "("  <parametros>  ")"<declaraciones> <instrucciones>  "fin_metodo"
     */
    
    
    
    /**
     * <declaracion_numero> ::= <identificador> "tipo" "numero" "=" <expresion>
     */
    private DeclaracionNumeroNodo parsearDeclaracionNumero(Analizador.Token identificador) throws ParseException {
        //////////////////////////////////////////////////////7    
       validarNoDuplicada(identificador.lexema, identificador.linea);
        /////////////////////////////////////////////////////////
        consumir("numero");
        consumir("=");
        
        String numero = tokenActual().toString();
       // System.out.println(numero.);
        ExpresionNodo valorInicial = parsearExpresion();
        validarDivisionesPorCero(valorInicial);
        
        //no se debería poder darle el valor de un sensor
            validarExpresionNoContieneSensores(valorInicial);
            
        return new DeclaracionNumeroNodo(identificador.lexema, valorInicial, 
                                       identificador.linea, identificador.columna);
    }
    
    /**
     * <declaracion_sensor> ::= <identificador> "tipo" "Sensor" "=" <numero>
     */
    private DeclaracionSensorNodo parsearDeclaracionSensor(Analizador.Token identificador) throws ParseException {
        validarNoDuplicada(identificador.lexema, identificador.linea);
        consumir("Sensor");
        consumir("=");
        Analizador.Token numeroToken = consumirTipo("NUMERO"); // Renombrado para claridad

        validarNumeroEntero(numeroToken.lexema, numeroToken.linea);

        int puerto = (int) Double.parseDouble(numeroToken.lexema);
        if (puerto > 255) {
            throw new ParseException("Valor asignado supera los límites físicos (0-255) para un pin, en línea: " + numeroToken.linea);
        }


        tablaSimbolos.add(new Analizador.EntradaTablaSimbolos(identificador.lexema, "Sensor", puerto, identificador.linea, identificador.columna));


        validarPinSensorNoDuplicado(puerto, numeroToken.linea, identificador.lexema);

        return new DeclaracionSensorNodo(identificador.lexema, puerto,
                                         identificador.linea, identificador.columna);
    }
    
    /**
     * <instrucciones> ::= <instruccion> <instrucciones> | ε
     */
    private void parsearInstrucciones(ProgramaNodo programa) throws ParseException {
        saltarComentarios();
        
        while (hayTokens() && !verificar("Terminar") && !verificar("fin")) {
            // Verificar si no estamos en una palabra de cierre
            Analizador.Token token = tokenActual();
            if (token != null && (token.lexema.startsWith("fin ") || token.lexema.equals("fin"))) {
                break;
            }
            
            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                programa.agregarInstruccion(instruccion);
            }
            saltarComentarios();
        }
    }
    //sobreescritura de este método para agregar las isntrucciones al nodo DeclaracionMetodoNodo
    private void parsearInstrucciones(DeclaracionMetodoNodo metodos) throws ParseException {
        saltarComentarios();
        //quite hayTokens() &&
        while ( !verificar("fin_metodo")) {
            // Verificar si no estamos en una palabra de cierre

            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                metodos.agregarInstruccion(instruccion);
            }
            saltarComentarios();
        }
    }
    
    /**
     * <instruccion> ::= <comando_movimiento> | <comando_actuador> | <comando_tiempo> 
     *                 | <estructura_control> | <asignacion> | <comando_control>
     */
    private NodoAST parsearInstruccion() throws ParseException {
        saltarComentarios();
        
        Analizador.Token token = tokenActual();
        if (token == null) {
            throw new ParseException("Se esperaba una instrucción");
        }
        
        String lexema = token.lexema;
        
        // Estructuras de control
        if (lexema.equals("si")) {
            return parsearBloqueSi();
        } else if (lexema.equals("para")) {
            return parsearBloquePara();
        } else if (lexema.equals("mientras")) {
            return parsearBloqueMientras();
        }
        // Comandos de movimiento
        else if (lexema.equals("mover_adelante") || lexema.equals("mover_atras") || 
                 lexema.equals("girar_izquierda") || lexema.equals("girar_derecha")) {
            return parsearComandoMovimiento();
        }
        // Comandos de actuador
        else if (lexema.equals("encender_led") || lexema.equals("apagar_led")) {
            return parsearComandoActuador();
        }
        // Comando de tiempo
        else if (lexema.equals("esperar")) {
            return parsearComandoTiempo();
        }
        // Comandos de control de flujo
        else if (lexema.equals("romper") || lexema.equals("detener")) {
            return parsearComandoControl();
        }
        // Asignación (identificador seguido de =)
        else if (verificarTipo("IDENTIFICADOR") && posicion + 1 < tokens.size() && 
                 tokens.get(posicion + 1).lexema.equals("=")) {
            return parsearAsignacion();
        }
        else {
            throw new ParseException("Instrucción no reconocida: '" + lexema + "' en línea " + token.linea);
        }
    }
    
    /**
     * <expresion> ::= <termino> | <expresion> "+" <termino> | <expresion> "-" <termino>
     * Implementado para manejar expresiones de N términos con precedencia correcta
     */
    private ExpresionNodo parsearExpresion() throws ParseException {
        ExpresionNodo primerTermino = parsearTermino();
        
        // Si no hay operadores de suma/resta, retornar el término simple
        if (!hayTokens() || (!verificarOperadorAritmetico("+") && !verificarOperadorAritmetico("-"))) {
            return primerTermino;
        }
        
        // Crear expresión aritmética para múltiples términos
        ExpresionAritmeticaNodo expresion = new ExpresionAritmeticaNodo(
            primerTermino.getLinea(), primerTermino.getColumna());
        expresion.agregarOperando(primerTermino);
        
        // Agregar todos los términos adicionales
        while (hayTokens() && (verificarOperadorAritmetico("+") || verificarOperadorAritmetico("-"))) {
            Analizador.Token operador = tokenActual();
            posicion++; // Consumir operador
            ExpresionNodo siguienteTermino = parsearTermino();
            
            expresion.agregarOperador(operador.lexema);
            expresion.agregarOperando(siguienteTermino);
        }
        
        
        return expresion;
    }
    
    /**
     * <termino> ::= <factor> | <termino> "*" <factor> | <termino> "/" <factor>
     * Implementado para manejar términos de N factores
     */
    private ExpresionNodo parsearTermino() throws ParseException {
        ExpresionNodo primerFactor = parsearFactor();
        
        // Si no hay operadores de multiplicación/división, retornar el factor simple
        if (!hayTokens() || (!verificarOperadorAritmetico("*") && !verificarOperadorAritmetico("/"))) {
            return primerFactor;
        }
        
        // Crear expresión aritmética para múltiples factores
        ExpresionAritmeticaNodo termino = new ExpresionAritmeticaNodo(
            primerFactor.getLinea(), primerFactor.getColumna());
        termino.agregarOperando(primerFactor);
        
        // Agregar todos los factores adicionales
        while (hayTokens() && (verificarOperadorAritmetico("*") || verificarOperadorAritmetico("/"))) {
            Analizador.Token operador = tokenActual();
            posicion++; // Consumir operador
            ExpresionNodo siguienteFactor = parsearFactor();
            
            termino.agregarOperador(operador.lexema);
            termino.agregarOperando(siguienteFactor);
        }
        
        return termino;
    }
    
    /**
     * <factor> ::= <numero> | <identificador> | "(" <expresion> ")" | <leer_sensor>
     */
    private ExpresionNodo parsearFactor() throws ParseException {
        saltarComentarios();
        
        Analizador.Token token = tokenActual();
        if (token == null) {
            throw new ParseException("Se esperaba un factor (número, identificador, paréntesis o leer_sensor)");
        }
        
        // Número
        if (verificarTipo("NUMERO")) {
            posicion++;
            double valor = Double.parseDouble(token.lexema);
            return new NumeroNodo(valor, token.linea, token.columna);
        }
        // Identificador
        else if (verificarTipo("IDENTIFICADOR")) {
            posicion++;
            
            //Aqui se verifica que haya sido declarada la variable
            validarVariableDeclarada(token.lexema, token.linea);
          /////////////////////////////////////
            
            return new IdentificadorNodo(token.lexema, token.linea, token.columna);
        }
        // Paréntesis - ACTUALIZADO para usar nuevos verificadores
        else if (verificarParentesis("(")) {
            consumir("(");
            ExpresionNodo expresion = parsearExpresion();
            consumir(")");
            return expresion;
        }
        // leer_sensor
        else if (verificar("leer_sensor")) {
            return parsearLeerSensor();
        }
        else {
            throw new ParseException("Factor inválido: '" + token.lexema + "' en línea " + token.linea);
        }
    }
    
    /**
     * <leer_sensor> ::= "leer_sensor" <identificador>
     */
    private LeerSensorNodo parsearLeerSensor() throws ParseException {
        Analizador.Token comando = consumir("leer_sensor");
        Analizador.Token sensor = consumirTipo("IDENTIFICADOR");
                /////////////////////////////////////////////////////////////////////////////7
        //Para usar la tabla de simbolos
        validarVariableDeclarada(sensor.lexema, sensor.linea);
        /////////////////////////////////////////////////////////////////////////////

        return new LeerSensorNodo(sensor.lexema, comando.linea, comando.columna);
    }
    
    /**
     * <bloque_si> ::= "si" <condicion> "entonces" <instrucciones> "fin si"
     */
    private SiNodo parsearBloqueSi() throws ParseException {
        Analizador.Token si = consumir("si");
        CondicionNodo condicion = parsearCondicion();
        consumir("entonces");
        
        SiNodo nodoSi = new SiNodo(condicion, si.linea, si.columna);
        
        // Parsear instrucciones hasta "fin si"
        saltarComentarios();
        while (hayTokens() && !verificar("fin si")) {
            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                nodoSi.agregarInstruccion(instruccion);
            }
            saltarComentarios();
        }
        
        consumir("fin si");
        return nodoSi;
    }
    
    /**
     * <bloque_para> ::= "para" <identificador> "=" <expresion> "hasta" <expresion> "hacer" <instrucciones> "fin para"
     */
    private ParaNodo parsearBloquePara() throws ParseException {
        Analizador.Token para = consumir("para");
        Analizador.Token variable = consumirTipo("IDENTIFICADOR");
        
        //En este bucle siempre se declara su propia variable de control
            validarNoDuplicada(variable.lexema, variable.linea);
        //por eso se debe verificar que la variable que se use no esté siendo usada
        
        
        consumir("=");
        ExpresionNodo inicio = parsearExpresion();
        consumir("hasta");
        ExpresionNodo fin = parsearExpresion();
        consumir("hacer");
        
        ParaNodo nodoPara = new ParaNodo(variable.lexema, inicio, fin, para.linea, para.columna);
        
        // Parsear instrucciones hasta "fin para"
        saltarComentarios();
        while (hayTokens() && !verificar("fin para")) {
            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                nodoPara.agregarInstruccion(instruccion);
            }
            saltarComentarios();
        }
        
        consumir("fin para");
        return nodoPara;
    }
    
    /**
     * <bloque_mientras> ::= "mientras" <condicion> "hacer" <instrucciones> "fin mientras"
     */
    private MientrasNodo parsearBloqueMientras() throws ParseException {
        Analizador.Token mientras = consumir("mientras");
        CondicionNodo condicion = parsearCondicion();
        consumir("hacer");
        
        MientrasNodo nodoMientras = new MientrasNodo(condicion, mientras.linea, mientras.columna);
        
        // Parsear instrucciones hasta "fin mientras"
        saltarComentarios();
        while (hayTokens() && !verificar("fin mientras")) {
            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                nodoMientras.agregarInstruccion(instruccion);
            }
            saltarComentarios();
        }
        
        consumir("fin mientras");
        return nodoMientras;
    }
    
    /**
     * <condicion> ::= <expresion> <operador_relacional> <expresion>
     */
    private CondicionNodo parsearCondicion() throws ParseException {
        ExpresionNodo izquierda = parsearExpresion();
        
        Analizador.Token operador = tokenActual();
        if (operador == null || !verificarTipo("OPERADOR_REL")) {
            throw new ParseException("Se esperaba un operador relacional (>, <, >=, <=, ==, !=) en línea " + 
                                   (operador != null ? operador.linea : "desconocida"));
        }
        posicion++; // Consumir operador
        
        ExpresionNodo derecha = parsearExpresion();
        validarDivisionesPorCero(izquierda);
        validarDivisionesPorCero(derecha);
        
        return new CondicionNodo(izquierda, operador.lexema, derecha, 
                               operador.linea, operador.columna);
    }
    
    /**
     * <comando_movimiento> ::= ("mover_adelante" | "mover_atras" | "girar_izquierda" | "girar_derecha") <expresion>
     */
    private ComandoMovimientoNodo parsearComandoMovimiento() throws ParseException {
        Analizador.Token comando = tokenActual();
        
        if (!verificarTipo("COMANDO_MOVIMIENTO")) {
            throw new ParseException("Se esperaba un comando de movimiento en línea " + comando.linea);
        }
        posicion++; // Consumir comando
        
        ExpresionNodo parametro = parsearExpresion();
        validarDivisionesPorCero(parametro);
        
        //no se pueden usar variables de tipo sensor, a menos que se le haga una asignacion previa
            validarExpresionNoContieneSensores(parametro);
        
        return new ComandoMovimientoNodo(comando.lexema, parametro, 
                                       comando.linea, comando.columna);
    }
    
    /**
     * <comando_actuador> ::= ("encender_led" | "apagar_led") <identificador>
     */
    private ComandoActuadorNodo parsearComandoActuador() throws ParseException {
        Analizador.Token comando = tokenActual();
        
        if (!verificarTipo("COMANDO_ACTUADOR")) {
            throw new ParseException("Se esperaba un comando de actuador en línea " + comando.linea);
        }
        posicion++; // Consumir comando
        
        Analizador.Token color = consumirTipo("IDENTIFICADOR");
       
        //eSTA LINEA SE PODRÍA QUITAR MÁS DELANTE CUANDO SE AGREGUEN LAS VARIABLES DE COLORES EN EL INTERPRETE. 
        
        validarColorValido(color.lexema, color.linea);
        
        ////////////////////////////////////
        return new ComandoActuadorNodo(comando.lexema, color.lexema, 
                                     comando.linea, comando.columna);
    }
    
    /**
     * <comando_tiempo> ::= "esperar" <expresion> <unidad_tiempo>
     */
    private ComandoTiempoNodo parsearComandoTiempo() throws ParseException {
        Analizador.Token esperar = consumir("esperar");
        ExpresionNodo duracion = parsearExpresion();
        
        //no se pueden usar variables de tipo Sensor directamente
        validarExpresionNoContieneSensores(duracion);
        validarDivisionesPorCero(duracion);
        
        
        
        Analizador.Token unidad = consumirTipo("UNIDAD_TIEMPO");
        
        return new ComandoTiempoNodo(duracion, unidad.lexema, 
                                   esperar.linea, esperar.columna);
    }
    
    /**
     * <asignacion> ::= <identificador> "=" <expresion>
     */
    private AsignacionNodo parsearAsignacion() throws ParseException {
        Analizador.Token variable = consumirTipo("IDENTIFICADOR");
        
        /////////////////////////////////////////////////////////////////////////////7
        //Para usar la tabla de simbolos
        validarVariableDeclarada(variable.lexema, variable.linea);
        /////////////////////////////////////////////////////////////////////////////
        
        
        //los pines de los sensores no se pueden reasignar
            validarNoEsSensor(variable.lexema, variable.linea);
        //////////////////
        consumir("=");
        ExpresionNodo valor = parsearExpresion();
        validarDivisionesPorCero(valor);
        
        //el tipo de variable debe ser compatible con la asignacion
            validarAsignacionTipoCompatible(variable.lexema, valor, variable.linea);
        
        return new AsignacionNodo(variable.lexema, valor, 
                                variable.linea, variable.columna);
    }
    
    /**
     * <comando_control> ::= "romper" | "detener"
     * Comandos que controlan el flujo de ejecución
     */
    private ComandoControlNodo parsearComandoControl() throws ParseException {
        Analizador.Token comando = tokenActual();
        
        if (!comando.lexema.equals("romper") && !comando.lexema.equals("detener")) {
            throw new ParseException("Se esperaba 'romper' o 'detener' en línea " + comando.linea);
        }
        
        posicion++; // Consumir comando
        
        return new ComandoControlNodo(comando.lexema, comando.linea, comando.columna);
    }
    
    /**
     * Excepción personalizada para errores de parsing
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
    
    
 //valida que el color sea uno de los reconocidos por el intérprete
 //SE PODRÁ QUITAR MÁS DELANTE, CUANDO SE AGREGUEN NUEVAS FUNCIONALIDADES
    
private void validarColorValido(String color, int linea) throws ParseException {
    Set<String> coloresValidos = Set.of("rojo", "verde", "azul", "amarillo");
    if (!coloresValidos.contains(color)) {
        throw new ParseException("Color inválido: '" + color + "' en línea " + linea + 
                               ". Colores válidos: " + coloresValidos);
    }
}

/**
 * Valida que un número sea entero (sin decimales)
 */
private void validarNumeroEntero(String numero, int linea) throws ParseException {
    try {
        // Verificar si contiene punto decimal
        if (numero.contains(".")) {
            throw new ParseException("El puerto del sensor debe ser un número entero, " +
                                   "no se permiten decimales: '" + numero + "' en línea " + linea);
        }
        
        // Verificar que sea un número válido
        Integer.parseInt(numero);
        
    } catch (NumberFormatException e) {
        throw new ParseException("Puerto de sensor inválido: '" + numero + "' en línea " + linea);
    }
}


/**
 * Valida que una variable no sea de tipo Sensor (no se puede reasignar)
 */
private void validarNoEsSensor(String nombreVariable, int linea) throws ParseException {
    // Buscar la variable en la tabla de símbolos
    Analizador.EntradaTablaSimbolos entrada = tablaSimbolos.stream()
        .filter(e -> e.getNombre().equals(nombreVariable))
        .findFirst()
        .orElse(null);
    
    if (entrada != null && entrada.getTipo().equals("Sensor")) {
        throw new ParseException("No se puede reasignar la variable '" + nombreVariable + 
                               "' porque es de tipo Sensor (línea " + linea + 
                               "). Los sensores tienen un pin fijo y no pueden cambiar durante la ejecución.");
    }
}


/**
 * Valida que una expresión no contenga variables de tipo Sensor
 */
private void validarExpresionNoContieneSensores(ExpresionNodo expresion) throws ParseException {
    validarExpresionNoContieneSensoresRecursivo(expresion);
}

/**
 * Método recursivo para validar expresiones complejas
 */
private void validarExpresionNoContieneSensoresRecursivo(ExpresionNodo nodo) throws ParseException {
    if (nodo instanceof IdentificadorNodo) {
        IdentificadorNodo identificador = (IdentificadorNodo) nodo;
        
        // Buscar en tabla de símbolos
        Analizador.EntradaTablaSimbolos entrada = tablaSimbolos.stream()
            .filter(e -> e.getNombre().equals(identificador.getNombre()))
            .findFirst()
            .orElse(null);
            
        if (entrada != null && entrada.getTipo().equals("Sensor")) {
            throw new ParseException("No se puede asignar la variable '" + identificador.getNombre() + 
                                   "' de tipo Sensor a una variable de tipo numero (línea " + nodo.getLinea() + 
                                   "). Use 'leer_sensor " + identificador.getNombre() + "' para obtener el valor del sensor.");
        }
    } 
    else if (nodo instanceof ExpresionAritmeticaNodo) {
        ExpresionAritmeticaNodo expresionArit = (ExpresionAritmeticaNodo) nodo;
        
        // Validar recursivamente todos los operandos
        for (ExpresionNodo operando : expresionArit.getOperandos()) {
            validarExpresionNoContieneSensoresRecursivo(operando);
        }
    }
    // NumeroNodo y LeerSensorNodo son válidos, no necesitan validación
}

/**
 * Valida que la asignación sea compatible con el tipo de la variable
 */
private void validarAsignacionTipoCompatible(String nombreVariable, ExpresionNodo valor, int linea) throws ParseException {
    // Buscar el tipo de la variable en la tabla de símbolos
    Analizador.EntradaTablaSimbolos entrada = tablaSimbolos.stream()
        .filter(e -> e.getNombre().equals(nombreVariable))
        .findFirst()
        .orElse(null);
    
    if (entrada != null && entrada.getTipo().equals("numero")) {
        // Si es tipo numero, no puede contener sensores en la expresión
        validarExpresionNoContieneSensores(valor);
    }
    //Espacio para validaciones futuras
}

}



