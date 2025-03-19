"""
Script principal para ejecutar la transformación de datos.

Este script utiliza la clase `TransformadorDatos` para cargar reglas desde un archivo Excel,
leer transacciones desde un archivo JSON, transformar los datos según las reglas, y generar
un archivo de texto con los datos transformados.

Steps:
1. Genera un identificador aleatorio para el archivo de salida.
2. Define las rutas de los archivos de entrada (Excel y JSON) y salida.
3. Verifica si la ruta de salida existe y, si no, genera un nuevo identificador aleatorio.
4. Ejecuta la transformación de datos utilizando la clase `TransformadorDatos`.

Attributes:
    ruta_excel (str): Ruta al archivo Excel que contiene las reglas de transformación.
    ruta_json (str): Ruta al archivo JSON que contiene las transacciones a transformar.
    ruta_salida (str): Ruta donde se generará el archivo de texto con los datos transformados.
"""

import random
import os

from java_vs_python.transformation.transform_data import TransformadorDatos

if __name__ == '__main__':    
    random_int = random.randint(1, 10000)
    ruta_excel = "./data/Reglas OPTIMA.xlsx"
    ruta_json = "./data/json_a_homologar.json"
    ruta_salida = f"./outputs/output_{random_int}.txt"
    if not os.path.exists(ruta_salida):  
        random_int = random.randint(1, 10000)
        ruta_salida = f"./outputs/output_{random_int}.txt"

    TransformadorDatos(ruta_excel, ruta_json, ruta_salida)