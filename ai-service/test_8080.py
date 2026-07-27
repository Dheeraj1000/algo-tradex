import requests

token = "eyJhbGciOiJIUzUxMiJ9.eyJ1bmFtZSI6IkRoZWVyYWoiLCJ1Y29kZSI6IjExMTI1MjEyMDIifQ.xxx" # Wait, I don't have the real JWT token.
# Let me just check the health endpoint or a public endpoint to see if 8080 is alive
res = requests.get("http://localhost:8080/actuator/health")
print(res.status_code, res.text)
