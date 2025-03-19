from utils.load import cargar_reglas, cargar_transacciones_json
from utils.transform import transformar_transacciones, generar_txt

class TransformadorDatos:
    """
    Clase para validar y transformar datos según reglas definidas en un archivo Excel.

    Esta clase carga las reglas desde un archivo Excel, lee las transacciones desde un archivo JSON,
    transforma los datos de las transacciones según las reglas, y genera un archivo de texto con los
    datos transformados.

    Attributes:
        reglas (dict): Diccionario con las reglas de transformación cargadas desde el archivo Excel.
        lista_fields_transformados (list): Lista de transacciones transformadas.
    """
    
    def __init__(self, ruta_excel, ruta_json, ruta_salida):
        """
        Inicializa la clase TransformadorDatos.

        Args:
            ruta_excel (str): Ruta al archivo Excel que contiene las reglas de transformación.
            ruta_json (str): Ruta al archivo JSON que contiene las transacciones a transformar.
            ruta_salida (str): Ruta donde se generará el archivo de texto con los datos transformados.

        Raises:
            ValueError: Si el archivo Excel no contiene las columnas requeridas.
            Exception: Si ocurre un error al leer o procesar los archivos Excel o JSON.
        """
        self.reglas = cargar_reglas(ruta_excel)
        transacciones = cargar_transacciones_json(ruta_json)
        self.lista_fields_transformados = transformar_transacciones(transacciones, self.reglas)
        generar_txt(self.lista_fields_transformados, ruta_salida)