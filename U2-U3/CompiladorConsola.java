import java.util.List;

public class CompiladorConsola {

    public static void main(String[] args) {

        String codigoFuenteDePrueba = "iniciar \n" +
            "num tipo numero = 25+5*5\n" +
            "sensor tipo Sensor = 170\n" +
            "distancia tipo numero = 15\n" +
            "Terminar"; 

        System.out.println("--- Iniciando Proceso de Compilación ---");
        System.out.println("Código Fuente:\n" + codigoFuenteDePrueba + "\n");

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
                System.err.println("Errores durante el análisis léxico:\n" + erroresLexicos.toString());
                return; 
            }

            List<Analizador.EntradaTablaSimbolos> tablaSimbolos = Analizador.getTablaSimbolosCompleta();

            // Instancia el AnalizadorSintactico
            AnalizadorSintactico analizadorSintactico = 
                new AnalizadorSintactico(tokens, tablaSimbolos);
            
            // Solo parsear el código para obtener el AST
            ProgramaNodo programaAST = analizadorSintactico.parsear(tokens, tablaSimbolos); 

            if (analizadorSintactico.getErrores().length() > 0) {
                System.err.println("Errores durante la compilación:\n");
                System.err.println(analizadorSintactico.getErrores().toString());
            } else if (programaAST != null) {
                System.out.println("Análisis sintáctico completado con éxito.\n");
                
                // === Generación de Código Intermedio (TAC) ===
                System.out.println("--- Generando Código Intermedio (Cuádruplos) ---\n");
                GeneradorCodigoIntermedio generadorCodigoIntermedio = new GeneradorCodigoIntermedio();
                // Pasa el generador al AST para que genere el código
                programaAST.generaCodigoIntermedio(generadorCodigoIntermedio);
                
                List<InstruccionTAC> cuadruplos = generadorCodigoIntermedio.getCodigo();
                generadorCodigoIntermedio.imprimirCuadruplosEnTabla();

                
            } else {
                System.err.println("Error desconocido: No se pudo construir el AST.");
            }

        } catch (Exception e) {
            System.err.println("¡Ocurrió un error inesperado durante la compilación!");
            e.printStackTrace(); 
        }

        System.out.println("\n--- Proceso de Compilación Finalizado ---");
    }
}