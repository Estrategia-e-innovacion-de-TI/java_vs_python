import pandas as pd

class ValidadorCampos:
    """
    Clase para validar campos según reglas extraídas de un archivo Excel.
    
    Contiene reglas de validación para cada campo incluyendo:
    - Nombre del campo (bb)
    - Código del campo (Campo destino)
    - Longitud
    - Tipo (CARACTER, NUMERICO)
    """
    
    def __init__(self, ruta_excel):
        """
        Inicializa el validador cargando las reglas desde un archivo Excel.
        
        Args:
            ruta_excel: Ruta al archivo Excel que contiene las reglas
        """
        self.reglas = {}
        self.cargar_reglas_desde_excel(ruta_excel)
    
    def cargar_reglas_desde_excel(self, ruta_excel):
        """
        Carga las reglas desde un archivo Excel.
        
        Args:
            ruta_excel: Ruta al archivo Excel que contiene las reglas
        """
        try:
            
            # Leer el archivo Excel y convertir a mayúsculas
            df = pd.read_excel(ruta_excel)
            df.columns = df.columns.str.upper()
            df = df.applymap(lambda x: x.upper() if isinstance(x, str) else x)
            
            # Verificar que las columnas requeridas estén presentes
            columnas_requeridas = ["BB", "CAMPO DESTINO", "TIPO", "LONGITUD"]
            for columna in columnas_requeridas:
                if columna not in df.columns:
                    raise ValueError(f"La columna '{columna}' no existe en el archivo Excel")
            
            # Procesar cada fila y extraer las reglas
            for _, fila in df.iterrows():
                nombre_campo = fila["BB"]
                codigo = fila["CAMPO DESTINO"]
                tipo = fila["TIPO"]
                longitud = int(fila["LONGITUD"])
                
                # Almacenar las reglas en el diccionario
                self.reglas[nombre_campo] = {
                    'codigo': codigo,
                    'longitud': longitud,
                    'tipo': tipo
                }
            
            print(f"Se cargaron {len(self.reglas)} reglas desde el archivo Excel")
        except Exception as e:
            print(f"Error al cargar las reglas desde Excel: {e}")
            raise
    
    