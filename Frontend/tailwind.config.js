/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts,scss,css}"],
  safelist: ['text-sm'],
  theme: {
    extend: {
      keyframes: {
        'slide-down': {
          '0%': {
            transform: 'translateY(-50%)',
            opacity: '0',
          },
          
          '100%': {
            transform: 'translateY(0)',
            opacity: '1',
          },
        },
      },
      animation: {
        'slide-down': 'slide-down 1s ease-out forwards',
      },
      fontFamily: {
        'be-vietnam': ['"Be Vietnam"', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
