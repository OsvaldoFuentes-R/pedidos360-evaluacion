export const environment = {
  production: false,
  // Agrega tu URL del backend aquí (puedes usar la IP de tu EC2 o el API Gateway)
  apiUrl: 'http://54.197.57.198:8080/api', 
  msalConfig: {
    auth: {
      clientId: 'd1a052a1-66c7-4847-b5e2-46fdb86c94f6', 
      authority: 'https://pedido360seba2026.ciamlogin.com/39eb9615-c754-4ef1-96f8-4f61494df782', 
      redirectUri: 'http://localhost:4200'
    }
  }
};