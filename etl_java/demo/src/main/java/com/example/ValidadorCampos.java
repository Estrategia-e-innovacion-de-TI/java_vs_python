package com.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidadorCampos {
    private final String rutaExcel;
    private final String rutaJson;
    private final String rutaSalida;
    private final Map<String, ReglaTransformacion> reglas = new HashMap<>();
    private final List<Map<String, String>> listaFieldsTransformados = new ArrayList<>();

    public ValidadorCampos(String rutaExcel, String rutaJson, String rutaSalida) {
        this.rutaExcel = rutaExcel;
        this.rutaJson = rutaJson;
        this.rutaSalida = rutaSalida;
        cargarReglasDesdeExcel();
        leerJson();
    }

    private void cargarReglasDesdeExcel() {
        // Cargar reglas desde archivo Excel
        try (FileInputStream fis = new FileInputStream(rutaExcel);

             // Crear un objeto Workbook para leer el archivo Excel
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Obtener la primera hoja del archivo
            Sheet sheet = workbook.getSheetAt(0);

            // Iterar sobre las filas de la hoja
            for (Row row : sheet) {
                // Saltar la primera fila (títulos)
                if (row.getRowNum() == 0) continue;

                // Obtener valores de las celdas
                String nombreCampo = obtenerValorCelda(row.getCell(0)).toUpperCase().trim();
                String campoDestino = obtenerValorCelda(row.getCell(1)).toUpperCase().trim();
                String tipo = obtenerValorCelda(row.getCell(3)).toUpperCase().trim();
                String longitudSrt = obtenerValorCelda(row.getCell(4)).trim();

                //convertir longitud a entero
                if (longitudSrt.isEmpty()) {
                    longitudSrt = "0";
                }
                int longitud = Integer.parseInt(longitudSrt);
                reglas.put(nombreCampo, new ReglaTransformacion(campoDestino, tipo, longitud));
            }

            System.out.println("Reglas cargadas: " + reglas.size());

        } catch (Exception e) {
            System.out.println("Error al cargar la reglas: " + e.getMessage());
        }
    }


    private void leerJson() {
        long startTotalTime = System.nanoTime(); // Total start time
        int transactionCount = 0;
        long totalProcessingTime = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(rutaJson), StandardCharsets.UTF_8)) {
            // Crear un objeto ObjectMapper para leer el JSON
            ObjectMapper objectMapper = new ObjectMapper();

            // Leer el JSON y obtener el nodo raíz
            JsonNode rootNode = objectMapper.readTree(reader);

            // Obtener la lista de transacciones
            JsonNode transactions = rootNode.path("body").path("transactions");
            System.out.println("==================================================");
            System.out.println("Transacciones encontradas: " + transactions.size());

            // Iterar sobre las transacciones
            for (JsonNode transaction : transactions) {
                long startTransactionTime = System.nanoTime(); // Start time for this transaction

                // Verificar si la transacción tiene campos
                if (!transaction.has("fields")) {
                    continue; // Saltar esta transacción si no tiene campos
                }

                // Crear un nuevo mapa para los campos transformados
                Map<String, String> nuevoFields = new LinkedHashMap<>();

                // Obtener los campos de la transacción
                JsonNode fields = transaction.get("fields");

                // Iterar sobre los campos
                fields.fieldNames().forEachRemaining(campoOriginal -> {
                    // Obtener el valor del campo
                    String valor = fields.get(campoOriginal).asText().toUpperCase().trim();

                    // Verificar si existe una regla de transformación para el campo
                    ReglaTransformacion regla = reglas.get(campoOriginal);

                    if (regla != null) {
                        String codigo = regla.getCodigo();
                        String tipo = regla.getTipo();
                        int longitud = regla.getLongitud();

                        // Transformar valor según reglas
                        String valorTransformado = transformarValor(valor, tipo, longitud);
                        nuevoFields.put(codigo, valorTransformado);
                    } else {
                        nuevoFields.put(campoOriginal, valor);
                    }
                });

                listaFieldsTransformados.add(nuevoFields);

                // Calcular el tiempo de procesamiento de la transacción
                long endTransactionTime = System.nanoTime();
                long transactionProcessingTime = endTransactionTime - startTransactionTime;
                totalProcessingTime += transactionProcessingTime;
                transactionCount++;
            }


            long endTotalTime = System.nanoTime();
            long totalExecutionTime = endTotalTime - startTotalTime;

            System.out.println("==================================================");

            if (transactionCount > 0) {
                double averageTransactionTime = (double) totalProcessingTime / transactionCount / 1_000.0; // Microseconds
                double totalExecutionTimeMs = totalExecutionTime / 1_000_000.0;

                System.out.printf("Tiempo promedio por transacción: %.2f µs%n", averageTransactionTime);
                System.out.printf("Total tiempo de Ejecución de los registros: %.2f ms%n", totalExecutionTimeMs);
            }

        } catch (IOException e) {
            System.out.println("Error al leer el archivo JSON: " + e.getMessage());
        }
    }

    private String transformarValor(String valor, String tipo, int longitud) {
        if (longitud <= 0) return valor; 

        // Formato de fechas
        switch (tipo) {
            case "FECINT", "DATETIME" -> {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");
                    Date fecha = inputFormat.parse(valor);
                    return outputFormat.format(fecha);
                } catch (Exception e) {
                    System.out.println("Error en fecha: " + valor);
                    return valor;
                }
            }


            // si el tipo es NUMERICO, se agregan ceros a la izquierda
            case "NUMERICO" -> {

                // Si el valor es null, lo convertimos a string vacío para evitar NullPointerException
                if (valor == null) {
                    valor = "";
                }

                // Verificamos si la longitud del valor es menor que la longitud deseada
                if (valor.length() < longitud) {
                    // Calculamos cuántos ceros necesitamos agregar
                    int cerosNecesarios = longitud - valor.length();

                    // Agregamos los ceros necesarios a la izquierda
                    // Agregamos el valor original

                    // Retornamos el string con los ceros a la izquierda
                    return "0".repeat(cerosNecesarios) + valor;
                }

                // Si el valor ya tiene la longitud necesaria o es mayor, lo retornamos sin cambios
                return valor;

                // Si el valor ya tiene la longitud necesaria o es mayor, lo retornamos sin cambios
            }
            case "ALFANUMERICO" -> {
                return String.format("%-" + longitud + "s", valor);
            }
        }

        return valor;
    }



    public void generarArchivoTxt(){

        if(listaFieldsTransformados.isEmpty()){
            System.out.println("No hay datos para exportar.");
            return;
        }

        File directorioSalida = new File(rutaSalida).getParentFile();
        if (directorioSalida != null && !directorioSalida.exists()) {
            directorioSalida.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaSalida, StandardCharsets.UTF_8))) {
            // Obtener columnas del primer registro
            List<String> columnas = new ArrayList<>(listaFieldsTransformados.get(0).keySet());

            // Escribir encabezados
            writer.write(String.join("\t", columnas));
            writer.newLine();

            // Escribir datos usando streams para mejor rendimiento
            listaFieldsTransformados.forEach(fila -> {
                try {
                    // Mapear cada columna a su valor correspondiente, usando "" si no existe
                    String lineaValores = columnas.stream()
                            .map(col -> fila.getOrDefault(col, ""))
                            .collect(Collectors.joining("\t"));

                    writer.write(lineaValores);
                    writer.newLine();
                } catch (IOException e) {
                    // Capturar y relanzar como RuntimeException para usar en expresión lambda
                    throw new UncheckedIOException("Error al escribir fila en archivo", e);
                }
            });

            System.out.println("Archivo generado exitosamente en: " + rutaSalida);

        } catch (UncheckedIOException e) {
            System.err.println("Error al procesar datos: " + e.getMessage());

        } catch (IOException e) {
            System.err.println("Error al escribir archivo: " + e.getMessage());
        }

    }

    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            // retornat sin espacios
            case STRING -> cell.getStringCellValue().toUpperCase().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()).toUpperCase().trim();
            default -> "";
        };
    }

    public static void main(String[] args) {

        String rutaExcel = "demo/data/Reglas OPTIMA.xlsx";
        //String rutaJson = "demo/data/json_a_homologar.json";
        String rutaJson = "demo/data/test_data_500000.json";
        String rutaSalida = "demo/outputs_" + new Random().nextInt(10000) + ".txt";

        // iniciar cálculo de tiempo
        long startTime = System.currentTimeMillis();

        ValidadorCampos validador = new ValidadorCampos(rutaExcel, rutaJson, rutaSalida);
        validador.generarArchivoTxt();

        // finalizar cálculo de tiempo
        long endTime = System.currentTimeMillis();
        long tiempoTotal = endTime - startTime;
        System.out.println("Tiempo total de ejecución: " + tiempoTotal + " ms");
    }
}


class ReglaTransformacion {
    private final String codigo;
    private final String tipo;
    private final int longitud;

    public ReglaTransformacion(String codigo, String tipo, int longitud) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.longitud = longitud;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public int getLongitud() {
        return longitud;
    }
}
