const STEPS = [
  {
    title: 'Configura',
    text: 'Scegli i personaggi che vuoi far crescere e racconta quanto tempo hai: bastano tre passi guidati.',
  },
  {
    title: 'Guarda il calendario',
    text: 'Eventi a tempo, Waveplate e obiettivi di crescita stanno insieme in un unico calendario.',
  },
  {
    title: 'Conferma il piano',
    text: "L'agente propone un piano e tu decidi se accettarlo. I calcoli li fa il programma, l'IA li spiega.",
  },
]

// The three steps of "come funziona" (D72): an ordered list, because the order is the meaning
function HowItWorks() {
  return (
    <section aria-labelledby="how-it-works-title" className="mt-5">
      <h2 id="how-it-works-title" className="h4 mb-3 terminal-section-title">
        Come funziona
      </h2>
      {/* list-style none removes the list semantics in Safari: role="list" puts them back */}
      <ol className="list-unstyled row row-cols-1 row-cols-md-3 g-3 mb-0" role="list" aria-labelledby="how-it-works-title">
        {STEPS.map((step, index) => (
          <li className="col" key={step.title}>
            <div className="card h-100">
              <div className="card-body">
                {/* The number of the step as a label of the terminal (D139); the list already says the order */}
                <p className="terminal-label mb-2" aria-hidden="true">
                  [ Passo_{String(index + 1).padStart(2, '0')} ]
                </p>
                <h3 className="h5 card-title">{step.title}</h3>
                <p className="card-text mb-0">{step.text}</p>
              </div>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}

export default HowItWorks
