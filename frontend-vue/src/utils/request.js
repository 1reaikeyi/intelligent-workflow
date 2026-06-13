import axios from 'axios'
const baseURL = '/api'

const instance = axios.create({
  baseURL,
  timeout: 10000
})

export default instance
export { baseURL }
